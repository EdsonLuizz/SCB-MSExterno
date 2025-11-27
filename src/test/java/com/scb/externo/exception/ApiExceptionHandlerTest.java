package com.scb.externo.exception;

import com.scb.externo.dto.Erro;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @SuppressWarnings("unused")
    private void dummy(String value) {
        // Método utilizado apenas para obter um MethodParameter no teste.
    }

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

        MockHttpServletRequest request = new MockHttpServletRequest();
        // se você quer testar o comportamento "normal" (lista de erros),
        // use uma URL diferente de /cobranca
        request.setRequestURI("/qualquer-coisa");

        ResponseEntity<List<Erro>> resp = handler.onValidation(ex, request);

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

    @Test
    void onValidation_quandoPostEmCobranca_deveRetornarMensagemGenerica() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult binding =
                new BeanPropertyBindingResult(target, "target");
        binding.addError(new FieldError("target", "valor", "mensagem"));

        Method m = this.getClass().getDeclaredMethod("dummy", String.class);
        MethodParameter mp = new MethodParameter(m, 0);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mp, binding);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/cobranca");

        ResponseEntity<List<Erro>> resp = handler.onValidation(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        assertEquals("422", resp.getBody().get(0).codigo());
        assertEquals("Dados Inválidos", resp.getBody().get(0).mensagem());
    }

    @Test
    void onValidation_quandoPostEmFilaCobranca_deveRetornarMensagemGenerica() throws Exception {
        // igual ao anterior, só troca a URI
        Object target = new Object();
        BeanPropertyBindingResult binding =
                new BeanPropertyBindingResult(target, "target");
        binding.addError(new FieldError("target", "valor", "mensagem"));

        Method m = this.getClass().getDeclaredMethod("dummy", String.class);
        MethodParameter mp = new MethodParameter(m, 0);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mp, binding);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/filaCobranca");

        ResponseEntity<List<Erro>> resp = handler.onValidation(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("Dados Inválidos", resp.getBody().get(0).mensagem());
    }

    @Test
    void onIllegalArgument_deveRetornar422ComMensagem() {
        IllegalArgumentException ex = new IllegalArgumentException("erro de negócio");

        ResponseEntity<List<Erro>> resp = handler.onIllegalArgument(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        assertEquals("422", resp.getBody().get(0).codigo());
        assertEquals("erro de negócio", resp.getBody().get(0).mensagem());
    }

    @Test
    void onValidation_quandoPostCobrancaOuFila_deveRetornarErroPadrao() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "campo", "mensagem qualquer"));

        Method method = this.getClass().getDeclaredMethod("dummy", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        // POST /cobranca
        MockHttpServletRequest reqCobranca = new MockHttpServletRequest("POST", "/cobranca");
        var respCobranca = handler.onValidation(ex, reqCobranca);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, respCobranca.getStatusCode());
        assertNotNull(respCobranca.getBody());
        assertEquals(1, respCobranca.getBody().size());
        Erro erroCobranca = respCobranca.getBody().get(0);
        assertEquals("422", erroCobranca.codigo());
        assertEquals("Dados Inválidos", erroCobranca.mensagem());

        // POST /filaCobranca
        MockHttpServletRequest reqFila = new MockHttpServletRequest("POST", "/filaCobranca");
        var respFila = handler.onValidation(ex, reqFila);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, respFila.getStatusCode());
        assertNotNull(respFila.getBody());
        assertEquals(1, respFila.getBody().size());
        Erro erroFila = respFila.getBody().get(0);
        assertEquals("422", erroFila.codigo());
        assertEquals("Dados Inválidos", erroFila.mensagem());
    }
}
