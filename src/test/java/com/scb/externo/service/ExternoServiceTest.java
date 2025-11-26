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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExternoServiceTest {

    private ExternoService service;
    private StripeGat gatewayMock;
    private MailgunGat mailgunGat;

    @BeforeEach
    void setUp() throws StripeException {
        gatewayMock = Mockito.mock(StripeGat.class);
        mailgunGat = Mockito.mock(MailgunGat.class);

        // 1) cria um PaymentIntent "fake"
        PaymentIntent piFake = Mockito.mock(PaymentIntent.class);
        Mockito.when(piFake.getId()).thenReturn("pi_test_123");

        // 2) configura o mock pra devolver esse PaymentIntent
        Mockito.when(gatewayMock.criarIntencaoDePagamento(
                Mockito.anyLong(),
                Mockito.anyString()
        )).thenReturn(piFake);

        // 3) injeta o mock no service
        service = new ExternoService(gatewayMock,mailgunGat);
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

}
