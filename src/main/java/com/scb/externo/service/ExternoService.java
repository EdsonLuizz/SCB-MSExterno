package com.scb.externo.service;

import com.scb.externo.gateway.MailgunGat;
import com.scb.externo.gateway.StripeGat;
import com.scb.externo.dto.*;
import com.scb.externo.exception.NotFoundException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class ExternoService {

    private static final String STATUS_AGUARDANDO_PAGAMENTO = "AGUARDANDO_PAGAMENTO";
    private static final String STATUS_EM_FILA = "EM_FILA";
    private static final String STATUS_FALHA = "FALHA";
    private static final String STATUS_PAGA = "PAGA";
    private static final Logger log = LoggerFactory.getLogger(ExternoService.class);
    private final AtomicLong seq = new AtomicLong(1);
    private final Map<Long, Cobranca> cobrancas = new ConcurrentHashMap<>();
    private final Queue<NovaCobranca> fila = new ConcurrentLinkedQueue<>();

    private final StripeGat stripeGateway;
    private final MailgunGat mailgunGateway;

    public ExternoService(StripeGat stripeGateway, MailgunGat mailgunGateway) {
        this.stripeGateway = stripeGateway;
        this.mailgunGateway = mailgunGateway;
    }

    public void restaurarBanco() {
        cobrancas.clear();
        fila.clear();
        seq.set(1);
    }

    public Email enviarEmail(NovoEmail req) {
        String email = req.email();

        if (email == null) {
            throw new IllegalArgumentException("Formato de e-mail inválido");
        }

        email = email.trim().toLowerCase();

        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            throw new IllegalArgumentException("Formato de e-mail inválido");
        }

        String dominio = email.substring(at + 1); // já está lower/trim

        boolean dominioValido =
                dominio.equals("gmail.com") ||
                        dominio.equals("hotmail.com") ||
                        dominio.equals("unirio.br") ||
                        dominio.endsWith(".unirio.br"); // aceita edu.unirio.br, aluno.unirio.br, etc.

        if (!dominioValido) {
            throw new IllegalArgumentException("Formato de e-mail inválido");
        }

        String assunto = "SCB - Notificação";

        mailgunGateway.enviarEmailSimples(email, assunto, req.mensagem()
        );

        //DTO de resposta
        long id = seq.getAndIncrement();
        return new Email(id, email, assunto, req.mensagem());
    }

    // Coloca a cobrança na fila

    public Cobranca incluirNaFila(NovaCobranca req) {
        fila.add(req);
        long id = seq.getAndIncrement();

        Instant agora = Instant.now();

        Cobranca c = new Cobranca(id, STATUS_EM_FILA, agora, null, req.valor(), req.ciclista(), null);

        cobrancas.put(id, c);
        return c;
    }

    // Cria uma cobrança imediata + PaymentIntent na Stripe.
    // O status final (PAGA/FALHA) será ajustado pelo webhook.

    public Cobranca criarCobranca(NovaCobranca req) {
        long id = seq.getAndIncrement();
        Instant agora = Instant.now();

        try {
            long valorEmCentavos = req.valor();
            PaymentIntent pi = stripeGateway.criarIntencaoDePagamento(
                    valorEmCentavos,
                    "Cobranca ciclista " + req.ciclista()
            );

            Cobranca c = new Cobranca(id, STATUS_AGUARDANDO_PAGAMENTO, agora, null, req.valor(), req.ciclista(), pi.getId());
            cobrancas.put(id, c);
            return c;

        } catch (StripeException e) {
            log.error("Erro ao criar PaymentIntent no Stripe para o ciclista {}.",
                    req.ciclista(), e);

            Cobranca c = new Cobranca(id, "FALHA_GATEWAY", agora, agora, req.valor(), req.ciclista(), null);
            cobrancas.put(id, c);
            return c;
        }
    }

    public Cobranca obterCobranca(Long id) {
        Cobranca c = cobrancas.get(id);
        if (c == null) throw new NotFoundException("Cobrança não encontrada");
        return c;
    }

    //método para validação de número de cartão de crédito

    private void validarValidade(YearMonth validade) {
        if (validade == null) {
            throw new IllegalArgumentException("Validade é obrigatória (yyyy-MM).");
        }

        YearMonth agora = YearMonth.now(ZoneId.of("America/Sao_Paulo"));
        if (validade.isBefore(agora)) {
            throw new IllegalArgumentException("Cartão expirado (validade " + validade + ")");
        }
    }

    public boolean validaNumero(String n) {


        if (n == null || n.isBlank()) {
            return false;
        }

        int s = 0;
        boolean alt = false;
        for (int i = n.length() - 1; i >= 0; i--) {
            int d = Character.digit(n.charAt(i), 10);
            if (d < 0) return false;
            if (alt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            s += d;
            alt = !alt;
        }
        return s % 10 == 0;
    }

    public boolean validaCartaoLuhn(NovoCartaoDeCredito cartao) {
        if (cartao == null) return false;

        validarValidade(cartao.validade());
        return validaNumero(cartao.numero());
    }

    // Chamado pelo webhook quando a Stripe informar que o pagamento foi aprovado.

    public void marcarComoPagoPorGatewayId(String gatewayId) {
        Cobranca original = cobrancas.values().stream()
                .filter(c -> gatewayId.equals(c.gatewayID()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada para gatewayId " + gatewayId));

        Cobranca atualizada = new Cobranca(original.id(), STATUS_PAGA, original.horaSolicitacao(), Instant.now(), original.valor(), original.ciclista(), original.gatewayID()
        );

        cobrancas.put(original.id(), atualizada);
    }

    // Chamado pelo webhook quando a Stripe informar que o pagamento falhou.

    public void marcarComoFalhaPorGatewayId(String gatewayId) {
        cobrancas.values().stream()
                .filter(c -> gatewayId.equals(c.gatewayID()))
                .findFirst()
                .ifPresent(c -> {
                    Cobranca atualizada = new Cobranca(c.id(), STATUS_FALHA, c.horaSolicitacao(), Instant.now(), c.valor(), c.ciclista(), c.gatewayID());
                    cobrancas.put(c.id(), atualizada);
                });
    }

    private boolean estaPendenteOuFalha(Cobranca c) {
        return STATUS_EM_FILA.equals(c.status()) || STATUS_FALHA.equals(c.status());
    }

    private void processarCobrancaDaFila(Cobranca c, List<Cobranca> atualizadas) {
        try {
            PaymentIntent piCriado = criarPaymentIntent(c);
            PaymentIntent piConfirmado =
                    stripeGateway.confirmaIntencaoPagamentoComCartaoTeste(piCriado.getId());

            tratarRetornoStripe(c, piConfirmado, atualizadas);
        } catch (StripeException e) {
            tratarErroProcessamento(c, atualizadas, e);
        }
    }

    private PaymentIntent criarPaymentIntent(Cobranca c) throws StripeException {
        return stripeGateway.criarIntencaoDePagamento(
                c.valor(), "Cobranca atrasada ciclista " + c.ciclista()
        );
    }

    private void tratarRetornoStripe(Cobranca original, PaymentIntent piConfirmado, List<Cobranca> atualizadas) {

        String statusStripe = piConfirmado.getStatus();
        String novoStatus = mapearStatusStripe(statusStripe);

        Instant agora = Instant.now();
        Instant horaFinalizacao = horaFinalizacao(novoStatus) ? agora : null;

        Cobranca atualizada = new Cobranca(original.id(), novoStatus, original.horaSolicitacao() != null ? original.horaSolicitacao() : agora, horaFinalizacao, original.valor(), original.ciclista(), piConfirmado.getId());

        cobrancas.put(original.id(), atualizada);
        atualizadas.add(atualizada);

        notificarEmail(atualizada, piConfirmado.getId());
    }

    private String mapearStatusStripe(String statusStripe) {
        return switch (statusStripe) {
            case "succeeded" -> STATUS_PAGA;
            case "requires_payment_method", "requires_action", "canceled" -> STATUS_FALHA;
            default -> STATUS_AGUARDANDO_PAGAMENTO;
        };
    }

    private boolean horaFinalizacao(String status) {
        return STATUS_PAGA.equals(status) || STATUS_FALHA.equals(status);
    }

    private void notificarEmail(Cobranca cobranca, String paymentIntentId) {
        if (!STATUS_PAGA.equals(cobranca.status())) {
            return;
        }

        String emailDestino = cobranca.ciclista();
        if (emailDestino != null && emailDestino.contains("@")) {
            String assunto = "SCB - Cobrança em atraso paga";
            String corpo = "Olá, sua cobrança em atraso no valor de " + cobranca.valor() + " centavos foi paga com sucesso. " + "ID da transação: " + paymentIntentId + ".";

            mailgunGateway.enviarEmailSimples(emailDestino, assunto, corpo);
        } else {
            log.warn("Cobrança {} marcada como PAGA, mas ciclista '{}' não é um e-mail válido.", cobranca.id(), cobranca.ciclista()
            );
        }
    }

    private void tratarErroProcessamento(Cobranca c, List<Cobranca> atualizadas, StripeException e) {

        log.error("Erro ao processar cobrança atrasada {} para ciclista {}.", c.id(), c.ciclista(), e);

        Instant agora = Instant.now();
        Cobranca falha = new Cobranca(c.id(), STATUS_FALHA, c.horaSolicitacao() != null ? c.horaSolicitacao() : agora, agora, c.valor(), c.ciclista(), c.gatewayID());
        cobrancas.put(c.id(), falha);
        atualizadas.add(falha);
    }

    public List<Cobranca> processarFila() {
        List<Cobranca> atualizadas = new ArrayList<>();

        cobrancas.values().stream().filter(this::estaPendenteOuFalha).forEach(c -> processarCobrancaDaFila(c, atualizadas));

        return atualizadas;
    }

    public Cobranca pagarCobranca(Long idCobranca) {

        Cobranca atual = obterCobranca(idCobranca);

        // lança NotFound se a cobrancça não existir
        if (atual.gatewayID() == null) {
            throw new IllegalStateException("Cobrança não possui gatewayID (PaymentIntent).");
        }

        try {
            PaymentIntent pi = stripeGateway.confirmaIntencaoPagamentoComCartaoTeste(atual.gatewayID());

            String novoStatus;
            switch (pi.getStatus()) {
                case "succeeded" -> novoStatus = STATUS_PAGA;
                case "requires_payment_method", "requires_action", "canceled" -> novoStatus = STATUS_FALHA;
                default -> novoStatus = STATUS_AGUARDANDO_PAGAMENTO;
            }

            Cobranca atualizada = new Cobranca(atual.id(), novoStatus, atual.horaSolicitacao(), Instant.now(), atual.valor(), atual.ciclista(), atual.gatewayID());
            cobrancas.put(atual.id(), atualizada);
            return atualizada;

        } catch (StripeException e) {
            if (e.getStripeError() != null) {
                log.error("Erro Stripe ao confirmar pagamento. msg={}, code={}, type={}, detail={}", e.getMessage(), e.getStripeError().getCode(), e.getStripeError().getType(), e.getStripeError().getMessage(), e);
            } else {
                log.error("Erro Stripe ao confirmar pagamento. msg={}", e.getMessage(), e);
            }

            Cobranca falha = new Cobranca(atual.id(), "FALHA GATEWAY", atual.horaSolicitacao(), Instant.now(), atual.valor(), atual.ciclista(), atual.gatewayID());
            cobrancas.put(atual.id(), falha);
            return falha;
        }
    }
}
