package com.scb.externo.service;

import com.scb.externo.dto.NovaCobranca;
import com.scb.externo.dto.NovoCartaoDeCredito;
import com.scb.externo.dto.Cobranca;
import com.scb.externo.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExternoServiceTest {

  ExternoService service = new ExternoService();

  @Test
  void luhn_valido_e_invalido() {
    assertTrue(service.validaCartao(new NovoCartaoDeCredito("4539578763621486","A","12/28","123")));
    assertFalse(service.validaCartao(new NovoCartaoDeCredito("1111111111111111","A","12/28","123")));
  }

  @Test
  void fila_processa_e_muda_status() {
    Cobranca c = service.incluirNaFila(new NovaCobranca("123", 2000L));
    java.util.List<Cobranca> processados = service.processarFila();
    assertFalse(processados.isEmpty());
    Cobranca atualizado = service.obterCobranca(c.id());
    assertNotEquals("EM_FILA", atualizado.status());
  }

  @Test
  void obter_nao_encontrada() {
    assertThrows(NotFoundException.class, () -> service.obterCobranca(999L));
  }
}
