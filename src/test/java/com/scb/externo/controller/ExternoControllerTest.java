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
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternoControllerTest {

    @InjectMocks
    private ExternoController controller;  

    @Mock
    private ExternoService service;

    @Test
    void enviarEmail_Retornar200ServiceOK() {
        NovoEmail req = new NovoEmail("fulana@ex.com","Cadastro realizado");
        Email email = new Email(1L, req.email(), req.mensagem(), "ENVIADO");

        when(service.enviarEmail(req)).thenReturn(email);

        ResponseEntity<Email> resp = controller.enviarEmail(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(email, resp.getBody());
        verify(service).enviarEmail(req);
    }

    @Test
    void enviarEmail_RetornarNotFound() {
        NovoEmail req = new NovoEmail("naoexiste@ex.com","msg");
        when(service.enviarEmail(req)).thenThrow(new NotFoundException("E-mail não existe"));

        assertThrows(NotFoundException.class, () -> controller.enviarEmail(req));
    }

    @Test
    void restaurarBanco_Retornar200() {
        ResponseEntity<String> resp = controller.restaurarBanco();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Banco restaurado", resp.getBody());
        verify(service).restaurarBanco();
    }

    @Test
    void pagarCobranca_deveChamarServicoEDevolverCobranca() {
        Cobranca resp = new Cobranca(30L, "PAGA", Instant.now(), Instant.now(), 300L, "ciclistaPago", "pi_pago");

        when(service.pagarCobranca(30L)).thenReturn(resp);

        var resultado = controller.pagarCobranca(30L);

        assertEquals(200, resultado.getStatusCode().value());
        assertEquals("PAGA", resultado.getBody().status());
        verify(service).pagarCobranca(30L);
    }
}
