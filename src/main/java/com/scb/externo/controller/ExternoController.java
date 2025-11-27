package com.scb.externo.controller;

import com.scb.externo.dto.*;
import com.scb.externo.service.ExternoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.scb.externo.dto.NovoCartaoDeCredito;
import com.scb.externo.dto.Erro;

import java.util.List;

@RestController
public class ExternoController {

  private final ExternoService service;
  public ExternoController(ExternoService service) {
      this.service = service;
  }

  @GetMapping("/restaurarBanco")
  public ResponseEntity<String> restaurarBanco() {
    service.restaurarBanco();
    return ResponseEntity.ok("Banco restaurado");
  }

  @PostMapping("/enviarEmail")
  public ResponseEntity<Email> enviarEmail(@Valid @RequestBody NovoEmail body) {
    return ResponseEntity.ok(service.enviarEmail(body));
  }

  //Cobrança

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

  @PostMapping("/cobranca/{idCobranca}/pagar")
  public ResponseEntity<Cobranca> pagarCobranca(@PathVariable Long idCobranca) {
    return ResponseEntity.ok(service.pagarCobranca(idCobranca));
  }

  //Endpoint para o UC16

  @PostMapping("/cobranca/fila/processar")
  public ResponseEntity<List<Cobranca>> processarCobrancasEmFila() {
      List<Cobranca> atualizadas = service.processarFila();
      return ResponseEntity.ok(atualizadas);
  }

  //Cartão de crédito

  @PostMapping("/validaCartaoDeCredito")
  public ResponseEntity<Object> validaCartaoDeCredito(@RequestBody @Valid NovoCartaoDeCredito req) {

      boolean valido = service.validaCartaoLuhn(req);

      if (!valido) {
          Erro erro = new Erro("422", "Cartão inválido");
          return ResponseEntity
                  .status(HttpStatus.UNPROCESSABLE_ENTITY)
                  .body(List.of(erro));
      }

      return ResponseEntity.ok(req);
  }

}
