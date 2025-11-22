package com.scb.externo.controller;

import com.scb.externo.dto.*;
import com.scb.externo.exception.NotFoundException;
import com.scb.externo.service.ExternoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternoControllerTest {

    @InjectMocks
    private ExternoController controller;  

    @Mock
    private ExternoService service;

    /*@Test
    void validaCartaoDeCredito_deveRetornar200QuandoValido() {
        NovoCartaoDeCredito dto =
                new NovoCartaoDeCredito("4539578763621486","Fulana","12/28","123");

        when(service.validaCartao(dto)).thenReturn(true);

        ResponseEntity<String> resp = controller.validaCartao(dto);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Dados atualizados", resp.getBody());
        verify(service).validaCartao(dto);
    }

    @Test
    void validaCartaoDeCredito_deveRetornar422QuandoInvalido() {
        NovoCartaoDeCredito dto =
                new NovoCartaoDeCredito("1111111111111111","Fulana","12/28","123");

        when(service.validaCartao(dto)).thenReturn(false);

        ResponseEntity<String> resp = controller.validaCartao(dto);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertTrue(resp.getBody().startsWith("422:"));
        verify(service).validaCartao(dto);
    }

    @Test
    void enviarEmail_deveRetornar200QuandoServiceOK() {
        NovoEmail req = new NovoEmail("fulana@ex.com","Cadastro realizado");
        Email email = new Email(1L, req.email(), req.mensagem(), "ENVIADO");

        when(service.enviarEmail(req)).thenReturn(email);

        ResponseEntity<Email> resp = controller.enviarEmail(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(email, resp.getBody());
        verify(service).enviarEmail(req);
    }*/

    @Test
    void enviarEmail_devePropagarNotFound() {
        NovoEmail req = new NovoEmail("naoexiste@ex.com","msg");
        when(service.enviarEmail(req))
                .thenThrow(new NotFoundException("E-mail não existe"));

        assertThrows(NotFoundException.class,
                () -> controller.enviarEmail(req));
    }

    @Test
    void restaurarBanco_deveRetornar200() {
        ResponseEntity<String> resp = controller.restaurarBanco();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Banco restaurado", resp.getBody());
        verify(service).restaurarBanco();
    }

    @Test
    void postCobranca_deveRetornarCobranca() {
        NovaCobranca req = new NovaCobranca("123", 1990L);
        Cobranca c = new Cobranca(1L, "123", 1990L, "SOLICITADA","gateway-fake-0");

        when(service.criarCobranca(req)).thenReturn(c);

        ResponseEntity<Cobranca> resp = controller.postCobranca(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(c, resp.getBody());
        verify(service).criarCobranca(req);
    }

    @Test
    void filaCobranca_deveRetornarCobrancaEmFila() {
        NovaCobranca req = new NovaCobranca("123", 2990L);
        Cobranca c = new Cobranca(2L, "123", 2990L, "EM_FILA", "gateway-fake-1");

        when(service.incluirNaFila(req)).thenReturn(c);

        ResponseEntity<Cobranca> resp = controller.filaCobranca(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(c, resp.getBody());
        verify(service).incluirNaFila(req);
    }

    @Test
    void getCobranca_deveRetornarCobranca() {
        Cobranca c = new Cobranca(99L, "123", 10L, "PAGA", "gateway-fake-2");
        when(service.obterCobranca(99L)).thenReturn(c);

        ResponseEntity<Cobranca> resp = controller.getCobranca(99L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(c, resp.getBody());
        verify(service).obterCobranca(99L);
    }

    /*@Test
    void processaCobrancasEmFila_deveRetornarLista() {
        Cobranca c1 = new Cobranca(1L, "123", 10L, "PAGA","gateway-fake-3");
        Cobranca c2 = new Cobranca(2L, "456", 20L, "FALHA", "gateway-fake-4");

        when(service.processarFila()).thenReturn(List.of(c1, c2));

        ResponseEntity<List<Cobranca>> resp = controller.processaCobrancasEmFila();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(List.of(c1, c2), resp.getBody());
        verify(service).processarFila();
    }*/

}
