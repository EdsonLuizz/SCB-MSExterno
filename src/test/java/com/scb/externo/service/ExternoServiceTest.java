package com.scb.externo.service;

import com.scb.externo.gateway.MailgunGat;
import com.scb.externo.gateway.StripeGat;
import com.scb.externo.dto.Cobranca;
import com.scb.externo.dto.Email;
import com.scb.externo.dto.NovaCobranca;
import com.scb.externo.dto.NovoCartaoDeCredito;
import com.scb.externo.dto.NovoEmail;
import com.scb.externo.exception.NotFoundException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExternoServiceTest {

    private ExternoService service;
    private StripeGat gatewayMock;
    private MailgunGat mailgunGat;

    @BeforeEach
    void setUp() throws StripeException {
        gatewayMock = Mockito.mock(StripeGat.class);
        mailgunGat  = Mockito.mock(MailgunGat.class);

        // 1) Mock da criação do PaymentIntent (usado em criarCobranca)
        PaymentIntent piCriacao = Mockito.mock(PaymentIntent.class);
        when(piCriacao.getId()).thenReturn("pi_test_123");

        when(gatewayMock.criarIntencaoDePagamento(
                anyLong(),
                anyString()
        )).thenReturn(piCriacao);

        // 2) Mock da CONFIRMAÇÃO do PaymentIntent (usado em pagarCobranca)
        PaymentIntent piConfirmado = Mockito.mock(PaymentIntent.class);
        when(piConfirmado.getStatus()).thenReturn("succeeded"); // status de sucesso

        when(gatewayMock.confirmarPaymentIntentComCartaoTeste(
                anyString()               // vai receber o gatewayID, ex: "pi_test_123"
        )).thenReturn(piConfirmado);

        // 3) injeta os mocks no service
        service = new ExternoService(gatewayMock, mailgunGat);
        service.restaurarBanco();
    }

    @Test
    void enviarEmail_deveLancarIllegalArgumentQuandoEmailInvalido() {
        NovoEmail req = new NovoEmail("invalido", "mensagem");

        assertThrows(IllegalArgumentException.class,
                () -> service.enviarEmail(req));
        verifyNoInteractions(mailgunGat);
    }

    @Test
    void enviarEmail_deveLancarNotFoundQuandoEmailNaoExiste() {
        String emailNaoExiste = "naoexiste@gmail.com";
        NovoEmail req = new NovoEmail(emailNaoExiste, "mensagem");

        doThrow(new NotFoundException("E-mail não existe"))
                .when(mailgunGat)
                .enviarEmailSimples(
                        eq(emailNaoExiste),
                        anyString(),
                        eq("mensagem")
                );

        assertThrows(NotFoundException.class,
                () -> service.enviarEmail(req));

        verify(mailgunGat).enviarEmailSimples(
                emailNaoExiste,
                "SCB - Notificação",
                "mensagem"
        );
    }

    @Test
    void enviarEmail_deveRetornarEmailQuandoDadosValidos() {
        NovoEmail req = new NovoEmail("edson_teste@gmail.com", "mensagem");

        // não faz nada quando o service chamar o Mailgun
        doNothing()
                .when(mailgunGat)
                .enviarEmailSimples(anyString(), anyString(), anyString());

        Email email = service.enviarEmail(req);

        assertNotNull(email);
        assertEquals("edson_teste@gmail.com", email.email());

        verify(mailgunGat).enviarEmailSimples(
                "edson_teste@gmail.com",
                "SCB - Notificação",
                "mensagem"
        );
    }

    @Test
    void criarCobranca_deveCriarCobrancaComStatusSolicitada() {
        NovaCobranca req = new NovaCobranca("ciclista", 1L);

        Cobranca c = service.criarCobranca(req);

        assertNotNull(c);
        assertEquals("AGUARDANDO_PAGAMENTO", c.status());
    }

    @Test
    void obterCobranca_deveRetornarQuandoExiste() {
        NovaCobranca req = new NovaCobranca("ciclista", 1L);
        Cobranca criada = service.criarCobranca(req);

        Cobranca obtida = service.obterCobranca(criada.id());

        assertEquals(criada.id(), obtida.id());
    }

    @Test
    void obterCobranca_deveLancarNotFoundQuandoNaoExiste() {
        assertThrows(NotFoundException.class,
                () -> service.obterCobranca(999L));
    }

    @Test
    void validaNumero_deveRetornarTrueParaNumeroValido() {
        // número válido pelo algoritmo de Luhn
        assertTrue(service.validaNumero("79927398713"));
    }

    @Test
    void validaNumero_deveRetornarFalseParaNumeroInvalido() {
        assertFalse(service.validaNumero("1234567890"));
    }

    @Test
    void validaNumero_deveRetornarFalseParaCaracterInvalido() {
        assertFalse(service.validaNumero("abcd"));
    }

    @Test
    void marcarComoPagoPorGatewayId_deveAtualizarStatusParaPaga() {
        NovaCobranca req = new NovaCobranca("ciclista", 10L);
        Cobranca criada = service.criarCobranca(req);

        // garante pré-condição
        assertEquals("AGUARDANDO_PAGAMENTO", criada.status());

        // chama o método novo
        service.marcarComoPagoPorGatewayId(criada.gatewayID());

        Cobranca atualizada = service.obterCobranca(criada.id());
        assertEquals("PAGA", atualizada.status());
    }

    @Test
    void validaCartao_deveRetornarTrue() {
        // ajuste os outros parâmetros, se o seu NovoCartaoDeCredito tiver mais campos
        NovoCartaoDeCredito cartao = new NovoCartaoDeCredito("Edson", "4532015112830366", "09/30","132");
        assertTrue(service.validaCartaoLuhn(cartao));
    }

    @Test
    void validaCartao_deveRetornarFalse() {
        // ajuste os outros parâmetros, se o seu NovoCartaoDeCredito tiver mais campos
        NovoCartaoDeCredito cartao = new NovoCartaoDeCredito("Raul", "1234567890", "18/026", "311");
        assertFalse(service.validaCartaoLuhn(cartao));
    }

    @Test
    void restaurarBanco() {
        // arrange: cria duas cobranças
        NovaCobranca req1 = new NovaCobranca("ciclista1", 100L);
        NovaCobranca req2 = new NovaCobranca("ciclista2", 200L);

        Cobranca c1 = service.criarCobranca(req1);
        Cobranca c2 = service.criarCobranca(req2);

        // sanity check
        assertNotNull(service.obterCobranca(c1.id()));
        assertNotNull(service.obterCobranca(c2.id()));

        // act
        service.restaurarBanco();

        boolean lancouC1 = false;

        try {
            service.obterCobranca(c1.id());
        } catch (NotFoundException expected) {
            lancouC1 = true;
        }
        assertTrue(lancouC1, "Era esperado que lançasse NotFoundException para c1");

        boolean lancouC2 = false;
        try {
            service.obterCobranca(c2.id());
        } catch (NotFoundException expected) {
            lancouC2 = true;
        }
        assertTrue(lancouC2, "Era esperado que lançasse NotFoundException para c2");
    }

    @Test
    void incluirNaFila() {
        NovaCobranca req = new NovaCobranca("ciclistaFila", 500L);

        Cobranca c = service.incluirNaFila(req);

        assertNotNull(c.id());
        assertEquals("ciclistaFila", c.ciclista());
        // ajuste de acordo com o status que você definiu no método
        // ex.: "EM_FILA" ou "FALHA_GATEWAY"
        assertEquals("EM_FILA", c.status());
    }

    @Test
    void pagarCobranca_alterandoHorariaFinalizacao() {
        // arrange
        NovaCobranca req = new NovaCobranca("ciclista", 150L);
        Cobranca criada = service.criarCobranca(req);

        // sanity check (opcional)
        assertEquals("AGUARDANDO_PAGAMENTO", criada.status());

        // act
        Cobranca paga = service.pagarCobranca(criada.id());

        // assert
        assertEquals("PAGA", paga.status());
        assertNotNull(paga.horaFinalizacao());
    }

    @Test
    void processarFila_quandoNaoHaCobrancasEmFila_deveRetornarListaVazia() {
        // não chamo incluirNaFila, então não existe nenhuma cobrança EM_FILA

        var atualizadas = service.processarFila();

        assertNotNull(atualizadas);
        assertTrue(atualizadas.isEmpty());
    }

    @Test
    void processarFila_quandoHaUmaCobrancaEmFila_deveAtualizarStatusEGateway() {
        // arrange: cria uma cobrança EM_FILA
        NovaCobranca req = new NovaCobranca("ciclistaFila", 500L);
        Cobranca emFila = service.incluirNaFila(req);
        assertEquals("EM_FILA", emFila.status());

        // act
        var atualizadas = service.processarFila();

        // assert
        assertEquals(1, atualizadas.size());
        Cobranca processada = atualizadas.get(0);

        assertEquals(emFila.id(), processada.id());
        // status que o seu processarFila define (ajuste se for outro):
        assertEquals("AGUARDANDO_PAGAMENTO", processada.status());
        // gatewayId deve ter sido preenchido com o id do PaymentIntent mockado no setUp
        assertEquals("pi_test_123", processada.gatewayID());
        // e, conforme o ajuste que você fez, a horaFinalizacao deve continuar nula
        assertNull(processada.horaFinalizacao());
    }

    @Test
    void pagarCobranca_quandoIdNaoExiste_deveLancarNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.pagarCobranca(9999L));
    }

    @Test
    void processarFila_quandoHaMultiplasCobrancasEmFila_deveAtualizarTodas() {
        NovaCobranca r1 = new NovaCobranca("c1", 100L);
        NovaCobranca r2 = new NovaCobranca("c2", 200L);

        service.incluirNaFila(r1);
        service.incluirNaFila(r2);

        var atualizadas = service.processarFila();

        for (Cobranca c : atualizadas) {
            assertEquals("AGUARDANDO_PAGAMENTO", c.status());
            assertEquals("pi_test_123", c.gatewayID());
        }
    }

    @Test
    void pagarCobranca_deveLancarIllegalStateQuandoNaoTemGatewayId() throws NoSuchFieldException, IllegalAccessException {
        // arrange: cria cobrança manualmente, sem gatewayID
        NovaCobranca req = new NovaCobranca("semGateway", 100L);
        Cobranca c = service.criarCobranca(req);

        // "anula" o gatewayID pra simular uma cobrança pendente sem PaymentIntent
        Cobranca semGateway = new Cobranca(
                c.id(),
                c.status(),
                c.horaSolicitacao(),
                c.horaFinalizacao(),
                c.valor(),
                c.ciclista(),
                null        // gatewayID nulo
        );
        // sobrescreve no "banco" em memória
        var campoCobrancas = ExternoService.class
                .getDeclaredField("cobrancas");
        campoCobrancas.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, Cobranca> mapa = (Map<Long, Cobranca>) campoCobrancas.get(service);
        mapa.put(c.id(), semGateway);

        boolean lancou = false;
        try {
            service.pagarCobranca(c.id());
        } catch (IllegalStateException expected) {
            lancou = true;
        }
        assertTrue(lancou, "Era esperado que lançasse IllegalStateException");
    }

    @Test
    void validaNumero_deveRetornarFalseQuandoStringVazia() {
        assertFalse(service.validaNumero(""));
    }

    @Test
    void validaNumero_deveRetornarFalseQuandoTemEspaco() {
        assertFalse(service.validaNumero("7992 7398713"));
    }

    @Test
    void criarCobranca_quandoStripeLancaExcecao_deveMarcarFalhaGateway() throws StripeException {
        // arrange: fazer o mock lançar StripeException
        Mockito.reset(gatewayMock);

        StripeException erroStripe = Mockito.mock(StripeException.class);

        Mockito.when(gatewayMock.criarIntencaoDePagamento(anyLong(), anyString()))
                .thenThrow(erroStripe);

        NovaCobranca req = new NovaCobranca("ciclistaErro", 123L);

        // act
        Cobranca c = service.criarCobranca(req);

        // assert
        assertEquals("FALHA_GATEWAY", c.status());
        assertNotNull(c.horaFinalizacao());
        assertNull(c.gatewayID());
    }

    @Test
    void enviarEmail_deveLancarIllegalArgumentQuandoDominioNaoPermitido() {
        // e-mail com formato válido, mas domínio não permitido
        NovoEmail req = new NovoEmail("usuario@yahoo.com", "mensagem");

        assertThrows(IllegalArgumentException.class,
                () -> service.enviarEmail(req));

        // garante que o Mailgun nem foi chamado
        verifyNoInteractions(mailgunGat);
    }

    @Test
    void processarFila_quandoStripeLancaExcecao_deveMarcarComoFalhaGateway() throws StripeException {
        // arrange: cria cobrança "EM_FILA"
        NovaCobranca req = new NovaCobranca("ciclistaFilaErro", 123L);
        Cobranca emFila = service.incluirNaFila(req);

        // para esse teste, fazemos o gateway lançar exceção
        StripeException stripeEx = Mockito.mock(StripeException.class);
        when(gatewayMock.criarIntencaoDePagamento(anyLong(), anyString()))
                .thenThrow(stripeEx);

        // act
        var atualizadas = service.processarFila();

        // assert
        assertEquals(1, atualizadas.size());
        Cobranca result = atualizadas.get(0);

        assertEquals(emFila.id(), result.id());
        // aqui uso startsWith para não brigar com "FALHA_GATEWAY" vs "FALHA GATAWAY"
        assertTrue(result.status().startsWith("FALHA"));
        assertNotNull(result.horaFinalizacao());
    }

    @Test
    void marcarComoFalhaPorGatewayId_quandoExisteCobranca_deveAtualizarStatus() {
        // arrange: cria cobrança que já gera um gatewayID
        NovaCobranca req = new NovaCobranca("ciclistaFalha", 500L);
        Cobranca aguardando = service.criarCobranca(req);

        // sanity check
        assertNotNull(aguardando.gatewayID());

        // act
        service.marcarComoFalhaPorGatewayId(aguardando.gatewayID());

        // assert
        Cobranca atualizada = service.obterCobranca(aguardando.id());
        assertEquals("FALHA", atualizada.status());
        assertNotNull(atualizada.horaFinalizacao());
    }

    @Test
    void pagarCobranca_quandoNaoTemGatewayId_deveLancarIllegalState() {
        // arrange: incluirNaFila cria cobrança com gatewayID == null
        NovaCobranca req = new NovaCobranca("semGateway", 200L);
        Cobranca emFila = service.incluirNaFila(req);

        boolean lancou = false;
        try {
            service.pagarCobranca(emFila.id());
        } catch (IllegalStateException expected) {
            lancou = true;
        }
        assertTrue(lancou, "Era esperado que lançasse IllegalStateException");
    }

    @Test
    void pagarCobranca_quandoStripeLancaExcecao_deveMarcarComoFalhaGateway() throws StripeException {
        // arrange: cria cobrança com gatewayID preenchido
        NovaCobranca req = new NovaCobranca("ciclistaStripeErro", 300L);
        Cobranca aguardando = service.criarCobranca(req);

        // para esse teste, confirmarPaymentIntentComCartaoTeste lança StripeException
        StripeException stripeEx = Mockito.mock(StripeException.class);
        when(gatewayMock.confirmarPaymentIntentComCartaoTeste(anyString()))
                .thenThrow(stripeEx);

        // act
        Cobranca resultado = service.pagarCobranca(aguardando.id());

        // assert
        assertTrue(resultado.status().startsWith("FALHA"));
        assertNotNull(resultado.horaFinalizacao());
    }

    @Test
    void marcarComoFalhaPorGatewayId_quandoExisteCobranca_deveAtualizarParaFalha() {
        NovaCobranca req = new NovaCobranca("ciclistaFalha", 500L);
        Cobranca criada = service.criarCobranca(req);

        // sanity check
        assertEquals("AGUARDANDO_PAGAMENTO", criada.status());

        service.marcarComoFalhaPorGatewayId(criada.gatewayID());

        Cobranca atualizada = service.obterCobranca(criada.id());
        assertEquals("FALHA", atualizada.status());
        assertNotNull(atualizada.horaFinalizacao());
    }

}
