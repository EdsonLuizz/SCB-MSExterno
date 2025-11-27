package com.scb.externo.config;

import com.stripe.Stripe;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class StripeConfigTest {

    @Test
    void init_deveDefinirApiKeyDaStripe_quandoApiKeyValida() throws Exception {
        StripeConfig config = new StripeConfig();

        // coloca o valor no campo privado "apiKey"
        Field field = StripeConfig.class.getDeclaredField("apiKey");
        field.setAccessible(true);
        field.set(config, "chave-teste");

        // garante estado inicial conhecido
        Stripe.apiKey = null;

        config.init();

        assertEquals("chave-teste", Stripe.apiKey);
    }

    @Test
    void init_naoDeveAlterarApiKey_quandoApiKeyVaziaOuEmBranco() throws Exception {
        StripeConfig config = new StripeConfig();

        // apiKey em branco
        Field field = StripeConfig.class.getDeclaredField("apiKey");
        field.setAccessible(true);
        field.set(config, "   ");

        // volta para o estado inicial da chave
        Stripe.apiKey = "valor-anterior";

        config.init();

        assertEquals("valor-anterior", Stripe.apiKey);
    }
}
