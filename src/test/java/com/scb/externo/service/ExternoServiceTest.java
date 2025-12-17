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

import java.time.YearMonth;
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

        // Mock da criação
        PaymentIntent piCriacao = Mockito.mock(PaymentIntent.class);
        when(piCriacao.getId()).thenReturn("pi_test_123");

        when(gatewayMock.criarIntencaoDePagamento(anyLong(), anyString())).thenReturn(piCriacao);

        // Mock da confirmação
        PaymentIntent piConfirmado = Mockito.mock(PaymentIntent.class);
        when(piConfirmado.getStatus()).thenReturn("succeeded");

        //Definindo ID
        when(piConfirmado.getId()).thenReturn("pi_test_123");

        when(gatewayMock.confirmaIntencaoPagamentoComCartaoTeste(anyString())).thenReturn(piConfirmado);

        service = new ExternoService(gatewayMock, mailgunGat);
        service.restaurarBanco();
    }

    @Test
    void enviarEmail_LancaIllegalArgument() {
        NovoEmail req = new NovoEmail("invalido", "mensagem");

        assertThrows(IllegalArgumentException.class, () -> service.enviarEmail(req));
        verifyNoInteractions(mailgunGat);
    }

    @Test
    void enviarEmail_LancaNotFound() {
        String emailNaoExiste = "naoexiste@gmail.com";
        NovoEmail req = new NovoEmail(emailNaoExiste, "mensagem");

        doThrow(new NotFoundException("E-mail não existe")).when(mailgunGat).enviarEmailSimples(eq(emailNaoExiste), anyString(), eq("mensagem"));

        assertThrows(NotFoundException.class, () -> service.enviarEmail(req));

        verify(mailgunGat).enviarEmailSimples(emailNaoExiste, "SCB - Notificação", "mensagem");
    }

    @Test
    void enviarEmail_RetornaEmail() {
        NovoEmail req = new NovoEmail("edson_teste@gmail.com", "mensagem");

        // não faz nada quando o service chamar o Mailgun
        doNothing().when(mailgunGat).enviarEmailSimples(anyString(), anyString(), anyString());

        Email email = service.enviarEmail(req);

        assertNotNull(email);
        assertEquals("edson_teste@gmail.com", email.email());

        verify(mailgunGat).enviarEmailSimples("edson_teste@gmail.com", "SCB - Notificação", "mensagem");
    }

    @Test
    void criarCobrancaComStatusSolicitacao() {
        NovaCobranca req = new NovaCobranca("ciclista", 1L);

        Cobranca c = service.criarCobranca(req);

        assertNotNull(c);
        assertEquals("AGUARDANDO_PAGAMENTO", c.status());
    }

    @Test
    void obterCobranca_RetornaQuandoExiste() {
        NovaCobranca req = new NovaCobranca("ciclista", 1L);
        Cobranca criada = service.criarCobranca(req);

        Cobranca obtida = service.obterCobranca(criada.id());

        assertEquals(criada.id(), obtida.id());
    }

    @Test
    void obterCobranca_deveLancarNotFoundQuandoNaoExiste() {
        assertThrows(NotFoundException.class, () -> service.obterCobranca(999L));
    }

    @Test
    void validaNumero_deveRetornarTrueParaNumeroValido() {
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
    void marcarComoPagoPorGatewayId() {
        NovaCobranca req = new NovaCobranca("ciclista", 10L);
        Cobranca criada = service.criarCobranca(req);

        // garante pré-condição
        assertEquals("AGUARDANDO_PAGAMENTO", criada.status());

        service.marcarComoPagoPorGatewayId(criada.gatewayID());

        Cobranca atualizada = service.obterCobranca(criada.id());
        assertEquals("PAGA", atualizada.status());
    }

    @Test
    void restaurarBanco() {

        NovaCobranca req1 = new NovaCobranca("ciclista1", 100L);
        NovaCobranca req2 = new NovaCobranca("ciclista2", 200L);

        Cobranca c1 = service.criarCobranca(req1);
        Cobranca c2 = service.criarCobranca(req2);


        assertNotNull(service.obterCobranca(c1.id()));
        assertNotNull(service.obterCobranca(c2.id()));

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
        assertEquals("EM_FILA", c.status());
    }

    @Test
    void pagarCobranca_alterandoHorariaFinalizacao() {

        NovaCobranca req = new NovaCobranca("ciclista", 150L);
        Cobranca criada = service.criarCobranca(req);

        assertEquals("AGUARDANDO_PAGAMENTO", criada.status());

        Cobranca paga = service.pagarCobranca(criada.id());

        assertEquals("PAGA", paga.status());
        assertNotNull(paga.horaFinalizacao());
    }

    @Test
    void processarFila_quandoNaoHaCobrancasEmFila_deveRetornarListaVazia() {

        var atualizadas = service.processarFila();

        assertNotNull(atualizadas);
        assertTrue(atualizadas.isEmpty());
    }

    @Test
    void processarFila_quandoHaUmaCobrancaEmFila_deveAtualizarStatusEGateway() {

        NovaCobranca req = new NovaCobranca("ciclistaFila", 500L);
        Cobranca emFila = service.incluirNaFila(req);
        assertEquals("EM_FILA", emFila.status());

        var atualizadas = service.processarFila();

        assertEquals(1, atualizadas.size());
        Cobranca processada = atualizadas.get(0);

        assertEquals(emFila.id(), processada.id());
        assertEquals("PAGA", processada.status());
        assertEquals("pi_test_123", processada.gatewayID());
        assertNotNull(processada.horaFinalizacao());
    }


    @Test
    void pagarCobranca_quandoIdNaoExiste_deveLancarNotFound() {
        assertThrows(NotFoundException.class, () -> service.pagarCobranca(9999L));
    }

    @Test
    void processarFila_quandoHaMultiplasCobrancasEmFila_deveAtualizarTodas() {
        NovaCobranca r1 = new NovaCobranca("c1", 100L);
        NovaCobranca r2 = new NovaCobranca("c2", 200L);

        service.incluirNaFila(r1);
        service.incluirNaFila(r2);

        var atualizadas = service.processarFila();

        for (Cobranca c : atualizadas) {
            assertEquals("PAGA", c.status());
            assertEquals("pi_test_123", c.gatewayID());
            assertNotNull(c.horaFinalizacao());
        }
    }

    @Test
    void pagarCobranca_deveLancarIllegalStateQuandoNaoTemGatewayId() throws NoSuchFieldException, IllegalAccessException {

        NovaCobranca req = new NovaCobranca("semGateway", 100L);
        Cobranca c = service.criarCobranca(req);

        Cobranca semGateway = new Cobranca(c.id(), c.status(), c.horaSolicitacao(), c.horaFinalizacao(), c.valor(), c.ciclista(), null);

        // sobrescreve no "banco" em memória
        var campoCobrancas = ExternoService.class.getDeclaredField("cobrancas");
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

        Mockito.reset(gatewayMock);

        StripeException erroStripe = Mockito.mock(StripeException.class);

        Mockito.when(gatewayMock.criarIntencaoDePagamento(anyLong(), anyString())).thenThrow(erroStripe);

        NovaCobranca req = new NovaCobranca("ciclistaErro", 123L);

        Cobranca c = service.criarCobranca(req);

        assertEquals("FALHA_GATEWAY", c.status());
        assertNotNull(c.horaFinalizacao());
        assertNull(c.gatewayID());
    }

    @Test
    void enviarEmail_deveLancarIllegalArgumentQuandoDominioNaoPermitido() {

        NovoEmail req = new NovoEmail("usuario@yahoo.com", "mensagem");

        assertThrows(IllegalArgumentException.class, () -> service.enviarEmail(req));

        // garante que o Mailgun nem foi chamado
        verifyNoInteractions(mailgunGat);
    }

    @Test
    void processarFila_quandoStripeLancaExcecao_deveMarcarComoFalhaGateway() throws StripeException {

        NovaCobranca req = new NovaCobranca("ciclistaFilaErro", 123L);
        Cobranca emFila = service.incluirNaFila(req);

        StripeException stripeEx = Mockito.mock(StripeException.class);
        when(gatewayMock.criarIntencaoDePagamento(anyLong(), anyString())).thenThrow(stripeEx);

        var atualizadas = service.processarFila();

        assertEquals(1, atualizadas.size());
        Cobranca result = atualizadas.get(0);

        assertEquals(emFila.id(), result.id());
        assertTrue(result.status().startsWith("FALHA"));
        assertNotNull(result.horaFinalizacao());
    }

    @Test
    void marcarComoFalhaPorGatewayId_quandoExisteCobranca_deveAtualizarStatus() {

        NovaCobranca req = new NovaCobranca("ciclistaFalha", 500L);
        Cobranca aguardando = service.criarCobranca(req);

        assertNotNull(aguardando.gatewayID());

        service.marcarComoFalhaPorGatewayId(aguardando.gatewayID());

        Cobranca atualizada = service.obterCobranca(aguardando.id());
        assertEquals("FALHA", atualizada.status());
        assertNotNull(atualizada.horaFinalizacao());
    }

    @Test
    void pagarCobranca_quandoNaoTemGatewayId_deveLancarIllegalState() {

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
        when(gatewayMock.confirmaIntencaoPagamentoComCartaoTeste(anyString())).thenThrow(stripeEx);

        Cobranca resultado = service.pagarCobranca(aguardando.id());

        assertTrue(resultado.status().startsWith("FALHA"));
        assertNotNull(resultado.horaFinalizacao());
    }

    @Test
    void marcarComoFalhaPorGatewayId_quandoExisteCobranca_deveAtualizarParaFalha() {
        NovaCobranca req = new NovaCobranca("ciclistaFalha", 500L);
        Cobranca criada = service.criarCobranca(req);

        assertEquals("AGUARDANDO_PAGAMENTO", criada.status());

        service.marcarComoFalhaPorGatewayId(criada.gatewayID());

        Cobranca atualizada = service.obterCobranca(criada.id());
        assertEquals("FALHA", atualizada.status());
        assertNotNull(atualizada.horaFinalizacao());
    }

    @Test
    void processarFila_quandoCobrancaJaFalha_deveTentarNovamenteEAprovar() throws Exception {

        NovaCobranca req = new NovaCobranca("ciclistaFalhaRetentativa", 500L);
        Cobranca emFila = service.incluirNaFila(req);

        var campoCobrancas = ExternoService.class.getDeclaredField("cobrancas");
        campoCobrancas.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, Cobranca> mapa = (Map<Long, Cobranca>) campoCobrancas.get(service);

        Cobranca falhaAnterior = new Cobranca(emFila.id(), "FALHA", emFila.horaSolicitacao(), emFila.horaFinalizacao(), emFila.valor(), emFila.ciclista(), emFila.gatewayID());
        mapa.put(emFila.id(), falhaAnterior);

        var atualizadas = service.processarFila();

        assertEquals(1, atualizadas.size());
        Cobranca atualizada = atualizadas.get(0);
        assertEquals("PAGA", atualizada.status());
        assertNotNull(atualizada.horaFinalizacao());
        assertNotNull(atualizada.gatewayID());
    }

    @Test
    void processarFila_quandoStripeRetornaRequiresPaymentMethod_deveMarcarFalha() throws StripeException {

        PaymentIntent piFalha = Mockito.mock(PaymentIntent.class);
        when(piFalha.getStatus()).thenReturn("requires_payment_method");
        when(piFalha.getId()).thenReturn("pi_req_123");
        when(gatewayMock.confirmaIntencaoPagamentoComCartaoTeste(anyString())).thenReturn(piFalha);

        NovaCobranca req = new NovaCobranca("ciclistaFalhaStatus", 600L);
        service.incluirNaFila(req);

        var atualizadas = service.processarFila();

        assertEquals(1, atualizadas.size());
        Cobranca result = atualizadas.get(0);
        assertEquals("FALHA", result.status());
        assertNotNull(result.horaFinalizacao());
        assertEquals("pi_req_123", result.gatewayID());
    }

    @Test
    void processarFila_quandoPagamentoSucessoECiclistaEhEmailValido_deveEnviarEmail() {

        NovaCobranca req = new NovaCobranca("cliente@teste.com", 800L);
        service.incluirNaFila(req);

        // zera interações anteriores com o Mailgun
        Mockito.clearInvocations(mailgunGat);

        service.processarFila();

        verify(mailgunGat, times(1)).enviarEmailSimples(eq("cliente@teste.com"), eq("SCB - Cobrança em atraso paga"), contains("ID da transação"));
    }

    @Test
    void processarFila_quandoPagamentoSucessoMasCiclistaNaoEhEmail_naoEnviaEmail() {

        NovaCobranca req = new NovaCobranca("ciclistaSemEmail", 900L);
        service.incluirNaFila(req);

        Mockito.clearInvocations(mailgunGat);

        service.processarFila();

        verifyNoInteractions(mailgunGat);
    }

}
