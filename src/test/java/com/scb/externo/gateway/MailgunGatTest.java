package com.scb.externo.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MailgunGatTest {

    @Test
    void enviarEmailSimples_quandoApiKeyNull_deveLancarIllegalArgument() {
        MailgunGat gat = new MailgunGat();

        // domain válido, apiKey continua null
        ReflectionTestUtils.setField(gat, "domain", "sandbox.mailgun.org");

        assertThrows(IllegalArgumentException.class, () ->
                gat.enviarEmailSimples("destino@teste.com", "Assunto", "Mensagem")
        );
    }

    @Test
    void enviarEmailSimples_quandoTudoOk_deveChamarRestTemplate() {
        MailgunGat gat = new MailgunGat();

        RestTemplate rtMock = mock(RestTemplate.class);
        ReflectionTestUtils.setField(gat, "restTemplate", rtMock);
        ReflectionTestUtils.setField(gat, "domain", "sandbox.mailgun.org");
        ReflectionTestUtils.setField(gat, "apiKey", "chave-fake");

        // STUB para o método que o código realmente usa: postForEntity
        when(rtMock.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.ACCEPTED));

        // não deve lançar exceção
        assertDoesNotThrow(() ->
                gat.enviarEmailSimples("destino@teste.com", "Assunto", "Mensagem")
        );

        // VERIFY também em postForEntity, não em exchange
        verify(rtMock).postForEntity(
                contains("sandbox.mailgun.org"),
                any(HttpEntity.class),
                eq(String.class)
        );
    }
}
