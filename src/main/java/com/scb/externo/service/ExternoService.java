package com.scb.externo.service;

import com.scb.externo.dto.*;
import com.scb.externo.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ExternoService {

  private final AtomicLong seq = new AtomicLong(1);
  private final Map<Long, Cobranca> cobrancas = new ConcurrentHashMap<>();
  private final Queue<NovaCobranca> fila = new ConcurrentLinkedQueue<>();
  private final Random rnd = new Random();

  public void restaurarBanco() {
    cobrancas.clear();
    fila.clear();
  }

  public Email enviarEmail(NovoEmail req) {
    if (!req.email().contains("@")) {
      throw new IllegalArgumentException("Formato de e-mail inválido");
    }
    if (req.email().startsWith("naoexiste")) {
      throw new NotFoundException("E-mail não existe");
    }
    long id = seq.getAndIncrement();
    return new Email(id, req.email(), req.mensagem(), "ENVIADO");
  }

  public Cobranca incluirNaFila(NovaCobranca req) {
    fila.add(req);
    long id = seq.getAndIncrement();
    Cobranca c = new Cobranca(id, req.ciclista(), req.valor(), "EM_FILA");
    cobrancas.put(id, c);
    return c;
  }

  public Cobranca criarCobranca(NovaCobranca req) {
    long id = seq.getAndIncrement();
    Cobranca c = new Cobranca(id, req.ciclista(), req.valor(), "SOLICITADA");
    cobrancas.put(id, c);
    return c;
  }

  public Cobranca obterCobranca(Long id) {
    Cobranca c = cobrancas.get(id);
    if (c == null) throw new NotFoundException("Cobrança não encontrada");
    return c;
  }

  public boolean valiaNumero(String n) {
    int s = 0; boolean alt = false;
    for (int i = n.length() - 1; i >= 0; i--) {
      int d = Character.digit(n.charAt(i), 10);
      if (d < 0) return false;
      if (alt) { d *= 2; if (d > 9) d -= 9; }
      s += d; alt = !alt;
    }
    return s % 10 == 0;
  }

  public boolean validaCartao(NovoCartaoDeCredito cartao) {
    return valiaNumero(cartao.numero());
  }

  public Cobranca processar(Cobranca c) {
    String status = rnd.nextBoolean() ? "PAGA" : "FALHA";
    Cobranca atualizada = new Cobranca(c.id(), c.ciclista(), c.valor(), status);
    cobrancas.put(c.id(), atualizada);
    return atualizada;
  }

  public java.util.List<Cobranca> processarFila() {
    return cobrancas.values().stream()
        .filter(c -> "EM_FILA".equals(c.status()))
        .map(this::processar)
        .toList();
  }
}
