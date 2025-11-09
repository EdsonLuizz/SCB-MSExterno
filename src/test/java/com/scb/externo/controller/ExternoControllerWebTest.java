package com.scb.externo.controller;

import com.scb.externo.dto.Email;
import com.scb.externo.dto.NovaCobranca;
import com.scb.externo.dto.NovoCartaoDeCredito;
import com.scb.externo.dto.NovoEmail;
import com.scb.externo.exception.NotFoundException;
import com.scb.externo.service.ExternoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExternoController.class)
class ExternoControllerWebTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;
  @MockBean ExternoService service;

  @Test
  void validaCartao_OK() throws Exception {
    Mockito.when(service.validaCartao(any(NovoCartaoDeCredito.class))).thenReturn(true);
    NovoCartaoDeCredito body = new NovoCartaoDeCredito("4539578763621486","Fulana","12/28","123");
    mvc.perform(post("/validaCartaoDeCredito")
        .contentType(MediaType.APPLICATION_JSON)
        .content(om.writeValueAsString(body)))
      .andExpect(status().isOk());
  }

  @Test
  void validaCartao_422() throws Exception {
    Mockito.when(service.validaCartao(any(NovoCartaoDeCredito.class))).thenReturn(false);
    NovoCartaoDeCredito body = new NovoCartaoDeCredito("1111111111111111","Fulana","12/28","123");
    mvc.perform(post("/validaCartaoDeCredito")
        .contentType(MediaType.APPLICATION_JSON)
        .content(om.writeValueAsString(body)))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void enviarEmail_OK() throws Exception {
    Mockito.when(service.enviarEmail(any(NovoEmail.class)))
      .thenReturn(new Email(1L,"fulana@ex.com","ok","ENVIADO"));
    NovoEmail body = new NovoEmail("fulana@ex.com","Cadastro realizado");
    mvc.perform(post("/enviarEmail")
        .contentType(MediaType.APPLICATION_JSON)
        .content(om.writeValueAsString(body)))
      .andExpect(status().isOk());
  }

  @Test
  void enviarEmail_404() throws Exception {
    Mockito.when(service.enviarEmail(any(NovoEmail.class)))
      .thenThrow(new NotFoundException("E-mail não existe"));
    NovoEmail body = new NovoEmail("naoexiste@ex.com","msg");
    mvc.perform(post("/enviarEmail")
        .contentType(MediaType.APPLICATION_JSON)
        .content(om.writeValueAsString(body)))
      .andExpect(status().isNotFound());
  }

  @Test
  void enviarEmail_422FormatoInvalido() throws Exception {
    Map<String, String> body = new HashMap<>();
    body.put("email", "fulana-ex-com"); // formato inválido
    body.put("mensagem", "x");
    mvc.perform(post("/enviarEmail")
        .contentType(MediaType.APPLICATION_JSON)
        .content(om.writeValueAsString(body)))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void cobranca_imediata_OK() throws Exception {
    NovaCobranca body = new NovaCobranca("123", 1990L);
    mvc.perform(post("/cobranca")
        .contentType(MediaType.APPLICATION_JSON)
        .content(om.writeValueAsString(body)))
      .andExpect(status().isOk());
  }

  @Test
  void filaCobranca_OK() throws Exception {
    NovaCobranca body = new NovaCobranca("123", 2990L);
    mvc.perform(post("/filaCobranca")
        .contentType(MediaType.APPLICATION_JSON)
        .content(om.writeValueAsString(body)))
      .andExpect(status().isOk());
  }

  @Test
  void processaFila_OK() throws Exception {
    mvc.perform(post("/processaCobrancasEmFila"))
      .andExpect(status().isOk());
  }

  @Test
  void getCobranca_404() throws Exception {
    Mockito.when(service.obterCobranca(99L))
      .thenThrow(new NotFoundException("Cobrança não encontrada"));
    mvc.perform(get("/cobranca/99"))
      .andExpect(status().isNotFound());
  }

  @Test
  void restaurarBanco_OK() throws Exception {
    mvc.perform(get("/restaurarBanco")).andExpect(status().isOk());
  }
}
