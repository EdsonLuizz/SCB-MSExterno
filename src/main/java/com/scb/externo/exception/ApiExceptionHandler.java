package com.scb.externo.exception;

import com.scb.externo.model.Erro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<List<Erro>> onValidation(MethodArgumentNotValidException ex) {
    List<Erro> erros = ex.getBindingResult().getFieldErrors().stream()
        .map(this::toErro).toList();
    return ResponseEntity.unprocessableEntity().body(erros);
  }

  private Erro toErro(FieldError fe) {
    String msg = fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Dados Inválidos";
    return new Erro(fe.getField(), msg);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Erro> onNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Erro("recurso", ex.getMessage()));
  }
}
