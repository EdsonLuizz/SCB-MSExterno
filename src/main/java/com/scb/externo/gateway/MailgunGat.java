package com.scb.externo.gateway;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class MailgunGat {

    @Value("${mailgun.domain:}")
    private String domain;

    @Value("${mailgun.from:postmaster@sandbox3ab49b3919a249fb95a5dff11c46de6e.mailgun.org}")
    private String from;

    @Value("${mailgun.api.key:}")
    private String apiKey;

    @PostConstruct
    public void debugConfigs() {
        System.out.println("Mailgun domain = '" + domain + "'");
        System.out.println("Mailgun from   = '" + from + "'");
        System.out.println("Mailgun apiKey prefix = '" +
                (apiKey == null ? "null" : apiKey.substring(0, Math.min(8, apiKey.length()))) + "...'");
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public void enviarEmailSimples(String para, String assunto, String corpo) {

        String url = "https://api.mailgun.net/v3/" + domain + "/messages";

        // de x para que o Mailgun espera
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", para);
        form.add("subject", assunto);
        form.add("text", corpo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // auth básica: usuário "api" + apiKey
        headers.setBasicAuth("api", apiKey);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);

        // dispara requisição
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        //testar enventuais erros no mailgun
        System.out.println("Resposta Mailgun: " + response.getStatusCode());
        System.out.println("Body Mailgun: " + response.getBody());

        //chec o status e logar/lançar exceção
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro ao enviar e-mail via Mailgun: " + response);
        }
    }
}
