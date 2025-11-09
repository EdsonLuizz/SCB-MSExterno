package com.scb.externo.controller;

import com.scb.externo.dto.*;
import com.scb.externo.service.ExternoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ExternoController {

  private final ExternoService service;
  public ExternoController(ExternoService service) { this.service = service; }

  @GetMapping("/restaurarBanco")
  public ResponseEntity<String> restaurarBanco() {
    service.restaurarBanco();
    return ResponseEntity.ok("Banco restaurado");
  }

  @PostMapping("/enviarEmail")
  public ResponseEntity<Email> enviarEmail(@Valid @RequestBody NovoEmail body) {
    return ResponseEntity.ok(service.enviarEmail(body));
  }

  @PostMapping("/cobranca")
  public ResponseEntity<Cobranca> postCobranca(@Valid @RequestBody NovaCobranca body) {
    return ResponseEntity.ok(service.criarCobranca(body));
  }

  @PostMapping("/processaCobrancasEmFila")
  public ResponseEntity<List<Cobranca>> processaCobrancasEmFila() {
    return ResponseEntity.ok(service.processarFila());
  }

  @PostMapping("/filaCobranca")
  public ResponseEntity<Cobranca> filaCobranca(@Valid @RequestBody NovaCobranca body) {
    return ResponseEntity.ok(service.incluirNaFila(body));
  }

  @GetMapping("/cobranca/{idCobranca}")
  public ResponseEntity<Cobranca> getCobranca(@PathVariable Long idCobranca) {
    return ResponseEntity.ok(service.obterCobranca(idCobranca));
  }

  @PostMapping("/validaCartaoDeCredito")
  public ResponseEntity<String> validaCartao(@Valid @RequestBody NovoCartaoDeCredito body) {
    if (service.validaCartao(body)) return ResponseEntity.ok("Dados atualizados");
    Erro erro = new Erro("numero", "Cartão inválido pelo algoritmo de Luhn");
    return ResponseEntity.unprocessableEntity().body("422: " + erro.mensagem());
  }
}
