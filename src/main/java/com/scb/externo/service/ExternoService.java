package com.scb.externo.service;

import com.scb.externo.gateway.StripeGat;
import com.scb.externo.dto.*;
import com.scb.externo.exception.NotFoundException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ExternoService {

    private final AtomicLong seq = new AtomicLong(1);
    private final Map<Long, Cobranca> cobrancas = new ConcurrentHashMap<>();
    private final Queue<NovaCobranca> fila = new ConcurrentLinkedQueue<>();

    private final StripeGat stripeGateway;

    public ExternoService(StripeGat stripeGateway) {
        this.stripeGateway = stripeGateway;
    }

    public void restaurarBanco() {
        cobrancas.clear();
        fila.clear();
        seq.set(1);
    }

    public Email enviarEmail(NovoEmail req) {
        if (!req.email().contains("@")) {
            throw new IllegalArgumentException("Formato de e-mail inválido");
        }
        if (req.email().startsWith("naoexiste")) {
            throw new NotFoundException("E-mail não existe");
        }
        long id = seq.getAndIncrement();
        return new Email(id, req.email(), req.mensagem(), "ENVIADO");
    }

    /**
     * Coloca a cobrança na fila, ainda sem criar PaymentIntent.
     * Você pode decidir mais tarde processar a fila chamando Stripe.
     */
    public Cobranca incluirNaFila(NovaCobranca req) {
        fila.add(req);
        long id = seq.getAndIncrement();
        Cobranca c = new Cobranca(id, req.ciclista(), req.valor(), "EM_FILA", null);
        cobrancas.put(id, c);
        return c;
    }

    /**
     * Cria uma cobrança imediata + PaymentIntent na Stripe.
     * Status inicial: AGUARDANDO_PAGAMENTO.
     * O status final (PAGA/FALHA) será ajustado pelo webhook.
     */
    public Cobranca criarCobranca(NovaCobranca req) {
        long id = seq.getAndIncrement();

        try {
            long valorEmCentavos = req.valor(); // se já estiver em centavos
            PaymentIntent pi = stripeGateway.criarIntencaoDePagamento(
                    valorEmCentavos,
                    "Cobranca ciclista " + req.ciclista()
            );

            Cobranca c = new Cobranca(
                    id,
                    req.ciclista(),
                    req.valor(),
                    "AGUARDANDO_PAGAMENTO",
                    pi.getId()           // gatewayId = id do PaymentIntent
            );
            cobrancas.put(id, c);
            return c;
        } catch (StripeException e) {
            // Se a integração com Stripe falhar, você pode marcar como FALHA_GATEWAY
            e.printStackTrace();
            Cobranca c = new Cobranca(
                    id,
                    req.ciclista(),
                    req.valor(),
                    "FALHA_GATEWAY",
                    null
            );
            cobrancas.put(id, c);
            return c;
        }
    }

    public Cobranca obterCobranca(Long id) {
        Cobranca c = cobrancas.get(id);
        if (c == null) throw new NotFoundException("Cobrança não encontrada");
        return c;
    }

    public boolean validaNumero(String n) {
        int s = 0;
        boolean alt = false;
        for (int i = n.length() - 1; i >= 0; i--) {
            int d = Character.digit(n.charAt(i), 10);
            if (d < 0) return false;
            if (alt) { d *= 2; if (d > 9) d -= 9; }
            s += d; alt = !alt;
        }
        return s % 10 == 0;
    }

    public boolean validaCartao(NovoCartaoDeCredito cartao) {
        return validaNumero(cartao.numero());
    }

    /**
     * Chamado pelo webhook quando a Stripe informar que o pagamento foi aprovado.
     */
    public void marcarComoPagoPorGatewayId(String gatewayId) {
        cobrancas.values().stream()
                .filter(c -> gatewayId.equals(c.gatewayID()))
                .findFirst()
                .ifPresent(c -> {
                    Cobranca atualizada = new Cobranca(c.id(), c.ciclista(), c.valor(), "PAGA", c.gatewayID());
                    cobrancas.put(c.id(), atualizada);
                });
    }

    /**
     * Chamado pelo webhook quando a Stripe informar que o pagamento falhou.
     */
    public void marcarComoFalhaPorGatewayId(String gatewayId) {
        cobrancas.values().stream()
                .filter(c -> gatewayId.equals(c.gatewayID()))
                .findFirst()
                .ifPresent(c -> {
                    Cobranca atualizada = new Cobranca(c.id(), c.ciclista(), c.valor(), "FALHA", c.gatewayID());
                    cobrancas.put(c.id(), atualizada);
                });
    }

    public List<Cobranca> processarFila() {
        List<Cobranca> atualizadas = new ArrayList<>();

        cobrancas.values().stream()
                .filter(c -> "EM_FILA".equals(c.status()))
                .forEach(c -> {
                    try {
                        // cria o PaymentIntent na Stripe
                        PaymentIntent pi = stripeGateway.criarIntencaoDePagamento(
                                c.valor(),                           // já em centavos
                                "Cobranca ciclista " + c.ciclista()
                        );

                        // marca como AGUARDANDO_PAGAMENTO e grava o gatewayID
                        Cobranca aguardando = new Cobranca(
                                c.id(),
                                c.ciclista(),
                                c.valor(),
                                "AGUARDANDO_PAGAMENTO",
                                pi.getId()          // id do PaymentIntent
                        );
                        cobrancas.put(c.id(), aguardando);
                        atualizadas.add(aguardando);

                    } catch (StripeException e) {
                        // se a chamada à Stripe falhar, marca como FALHA_GATEWAY
                        Cobranca falhaGateway = new Cobranca(
                                c.id(),
                                c.ciclista(),
                                c.valor(),
                                "FALHA_GATEWAY",
                                null
                        );
                        cobrancas.put(c.id(), falhaGateway);
                        atualizadas.add(falhaGateway);
                    }
                });

        return atualizadas;
    }

    public Cobranca pagarCobranca(Long idCobranca) {
        Cobranca atual = obterCobranca(idCobranca); // já lança NotFound se não existir

        if (atual.gatewayID() == null) {
            throw new IllegalStateException("Cobrança não possui gatewayID (PaymentIntent).");
        }

        try {
            PaymentIntent pi = stripeGateway.confirmarPaymentIntentComCartaoTeste(atual.gatewayID());

            String novoStatus;
            switch (pi.getStatus()) {
                case "succeeded" -> novoStatus = "PAGA";
                case "requires_payment_method",
                     "requires_action",
                     "canceled" -> novoStatus = "FALHA";
                default -> novoStatus = "AGUARDANDO_PAGAMENTO";
            }

            Cobranca atualizada = new Cobranca(
                    atual.id(),
                    atual.ciclista(),
                    atual.valor(),
                    novoStatus,
                    atual.gatewayID()
            );
            cobrancas.put(atual.id(), atualizada);
            return atualizada;

        } catch (StripeException e) {
            System.err.println("ERRO STRIPE AO CONFIRMAR PAGAMENTO:");
            System.err.println("Mensagem: " + e.getMessage());
            if (e.getStripeError() != null) {
                System.err.println("Código:   " + e.getStripeError().getCode());
                System.err.println("Tipo:     " + e.getStripeError().getType());
                System.err.println("Detalhe:  " + e.getStripeError().getMessage());
            }

            Cobranca falha = new Cobranca(
                    atual.id(),
                    atual.ciclista(),
                    atual.valor(),
                    "FALHA_GATEWAY",
                    atual.gatewayID()
            );
            cobrancas.put(atual.id(), falha);
            return falha;
        }
    }
}
