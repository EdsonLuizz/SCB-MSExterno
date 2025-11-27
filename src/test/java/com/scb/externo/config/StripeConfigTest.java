package com.scb.externo.config;

import com.stripe.Stripe;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class StripeConfigTest {

    @Test
    void init_deveDefinirApiKeyDaStripe_quandoApiKeyValida() throws Exception {
        StripeConfig config = new StripeConfig();

        // injeta o valor no campo privado "apiKey"
        Field field = StripeConfig.class.getDeclaredField("apiKey");
        field.setAccessible(true);
        field.set(config, "chave-teste");

        // garante estado inicial conhecido
        Stripe.apiKey = null;

        // act
        config.init();

        // assert
        assertEquals("chave-teste", Stripe.apiKey);
    }

    @Test
    void init_naoDeveAlterarApiKey_quandoApiKeyVaziaOuEmBranco() throws Exception {
        StripeConfig config = new StripeConfig();

        // apiKey em branco -> cai no "return" do init()
        Field field = StripeConfig.class.getDeclaredField("apiKey");
        field.setAccessible(true);
        field.set(config, "   ");

        // estado anterior da Stripe
        Stripe.apiKey = "valor-anterior";

        // act
        config.init();

        // assert: continua com o valor anterior
        assertEquals("valor-anterior", Stripe.apiKey);
    }
}
