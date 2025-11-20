package com.scb.externo.service;

import com.scb.externo.dto.Cobranca;
import com.scb.externo.dto.Email;
import com.scb.externo.dto.NovaCobranca;
import com.scb.externo.dto.NovoCartaoDeCredito;
import com.scb.externo.dto.NovoEmail;
import com.scb.externo.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExternoServiceTest {

    private ExternoService service;

    @BeforeEach
    void setUp() {
        service = new ExternoService();
        service.restaurarBanco();
    }

    @Test
    void enviarEmail_deveLancarIllegalArgumentQuandoEmailInvalido() {
        NovoEmail req = new NovoEmail("invalido", "mensagem");

        assertThrows(IllegalArgumentException.class,
                () -> service.enviarEmail(req));
    }

    @Test
    void enviarEmail_deveLancarNotFoundQuandoEmailNaoExiste() {
        NovoEmail req = new NovoEmail("naoexiste@teste.com", "mensagem");

        assertThrows(NotFoundException.class,
                () -> service.enviarEmail(req));
    }

    @Test
    void enviarEmail_deveRetornarEmailQuandoDadosValidos() {
        NovoEmail req = new NovoEmail("teste@exemplo.com", "mensagem");

        Email email = service.enviarEmail(req);

        assertNotNull(email);
        assertEquals("teste@exemplo.com", email.email());
        assertEquals("ENVIADO", email.status());
    }

    @Test
    void incluirNaFila_deveCriarCobrancaComStatusEmFila() {
        NovaCobranca req = new NovaCobranca("ciclista", 10L);

        Cobranca c = service.incluirNaFila(req);

        assertNotNull(c);
        assertEquals("EM_FILA", c.status());
    }

    @Test
    void criarCobranca_deveCriarCobrancaComStatusSolicitada() {
        NovaCobranca req = new NovaCobranca("ciclista", 1L);

        Cobranca c = service.criarCobranca(req);

        assertNotNull(c);
        assertEquals("SOLICITADA", c.status());
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
    void processar_deveAtualizarStatusParaPagaOuFalha() {
        NovaCobranca req = new NovaCobranca("ciclista", 10L);
        Cobranca criada = service.criarCobranca(req);

        Cobranca processada = service.processar(criada);

        assertTrue(Set.of("PAGA", "FALHA").contains(processada.status()));
    }

    @Test
    void processarFila_deveProcessarCobrancasEmFila() {
        NovaCobranca req1 = new NovaCobranca("ciclista1", 10L);
        NovaCobranca req2 = new NovaCobranca("ciclista2", 1L);

        service.incluirNaFila(req1);
        service.incluirNaFila(req2);

        List<Cobranca> processadas = service.processarFila();

        assertEquals(2, processadas.size());
        processadas.forEach(c ->
                assertTrue(Set.of("PAGA", "FALHA").contains(c.status()))
        );
    }
    @Test
    void validaCartao_deveRetornarTrue() {
        // ajuste os outros parâmetros, se o seu NovoCartaoDeCredito tiver mais campos
        NovoCartaoDeCredito cartao = new NovoCartaoDeCredito("79927398713", "Edson", "29/11","132");
        assertTrue(service.validaCartao(cartao));
    }

    @Test
    void validaCartao_deveRetornarFalse() {
        // ajuste os outros parâmetros, se o seu NovoCartaoDeCredito tiver mais campos
        NovoCartaoDeCredito cartao = new NovoCartaoDeCredito("1234567890", "Raul", "18/07", "311");
        assertFalse(service.validaCartao(cartao));
    }

}
