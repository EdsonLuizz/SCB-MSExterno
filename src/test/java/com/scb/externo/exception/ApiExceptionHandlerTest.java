package com.scb.externo.exception;

import com.scb.externo.dto.Erro;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    // método “fake” só para criar o MethodParameter
    @SuppressWarnings("unused")
    private void dummy(String value) { }

    @Test
    void onValidation_deveRetornar422ComListaDeErros() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "target");

        bindingResult.addError(
                new FieldError("target", "campo", "mensagem de erro")
        );

        Method method = this.getClass().getDeclaredMethod("dummy", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<List<Erro>> resp = handler.onValidation(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void onNotFound_deveRetornar404() {
        NotFoundException ex = new NotFoundException("não encontrado");

        ResponseEntity<Erro> resp = handler.onNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }
}
