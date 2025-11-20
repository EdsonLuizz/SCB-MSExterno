package com.scb.externo.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotFoundExceptionTest {

    @Test
    void deveConterMensagemInformada() {
        NotFoundException ex = new NotFoundException("mensagem de erro");
        assertEquals("mensagem de erro", ex.getMessage());
    }
}
