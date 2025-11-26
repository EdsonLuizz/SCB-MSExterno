package com.scb.externo.exception;

import com.scb.externo.dto.Erro;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 422 – erros de validação (Bean Validation @NotNull, @Min, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<Erro>> onValidation(MethodArgumentNotValidException ex,
                                                   HttpServletRequest request) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean ehPost = "POST".equalsIgnoreCase(method);
        boolean ehCobrancaSimples   = "/cobranca".equals(path);
        boolean ehFilaDeCobranca    = "/filaCobranca".equals(path);

        //Regra específica para POST /cobranca e /filacobranca***
        if (ehPost && (ehCobrancaSimples || ehFilaDeCobranca)) {
            Erro erro = new Erro("422", "Dados Inválidos");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(List.of(erro));
        }

        //Comportamento para erros padrões para os demais endpoints
        List<Erro> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErro)
                .toList();

        return ResponseEntity.unprocessableEntity().body(erros);
    }

    private Erro toErro(FieldError fe) {
        String msg = fe.getDefaultMessage() != null ? fe.getDefaultMessage(): "Dados inválidos";
        //o codigo continua sendo o nome do campo para os outros endpoints
        return new Erro(fe.getField(), msg);
    }

    // 422 – e-mail com formato inválido (regra de negócio)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<List<Erro>> onIllegalArgument(IllegalArgumentException ex) {
        Erro erro = new Erro("422", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(List.of(erro));
    }

    // 404 – e-mail não existe
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Erro> onNotFound(NotFoundException ex) {
        Erro erro = new Erro("404", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(erro);
    }
}
