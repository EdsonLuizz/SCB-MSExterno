package com.scb.externo.gateway;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scb.externo.exception.MailgunException;

@Component
public class MailgunGat {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(MailgunGat.class);

    @Value("${mailgun.domain:}")
    private String domain;

    @Value("${mailgun.from:}")
    private String from;

    @Value("${mailgun.api.key:}")
    private String apiKey;

    @PostConstruct
    public void debugConfigs() {
        // Evita logar a chave inteira – só um prefixo
        if (log.isDebugEnabled()) {

            String apiPrefix = (apiKey == null) ? "null" : apiKey.substring(0, Math.min(8, apiKey.length()));

            log.debug("Mailgun domain='{}'", domain);
            log.debug("Mailgun from='{}'", from);
            log.debug("Mailgun apiKey prefix='{}...'", apiPrefix);
        }
    }

    public void enviarEmailSimples(String para, String assunto, String corpo) {

        String url = "https://api.mailgun.net/v3/" + domain + "/messages";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", para);
        form.add("subject", assunto);
        form.add("text", corpo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("api", apiKey);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        log.info("Resposta Mailgun: status={}", response.getStatusCode());
        log.debug("Body Mailgun: {}", response.getBody());

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new MailgunException("Erro ao enviar e-mail via Mailgun: " + response);
        }
    }
}
