package com.scb.externo.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MailgunGatTest {

    @Test
    void enviarEmailSimples_semApiKey_deveLancarIllegalArgumentException() {
        MailgunGat gat = new MailgunGat(); // apiKey fica null mesmo

        assertThrows(IllegalArgumentException.class, () ->
                gat.enviarEmailSimples(
                        "destinatario@teste.com",
                        "Assunto de teste",
                        "Corpo da mensagem"
                )
        );
    }
}