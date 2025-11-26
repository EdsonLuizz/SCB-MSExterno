package com.scb.externo.gateway;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StripeGatTest {

    @Test
    void criarIntencaoDePagamento_deveDelegarParaPaymentIntentCreate() throws StripeException {
        StripeGat gat = new StripeGat();

        PaymentIntent piMock = mock(PaymentIntent.class);

        try (MockedStatic<PaymentIntent> paymentIntent =
                     Mockito.mockStatic(PaymentIntent.class)) {

            paymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            PaymentIntent retorno =
                    gat.criarIntencaoDePagamento(1234L, "descricao");

            // garante que chamou o create e retornou o mesmo objeto
            paymentIntent.verify(
                    () -> PaymentIntent.create(any(PaymentIntentCreateParams.class)));
            assertSame(piMock, retorno);
        }
    }

    @Test
    void confirmarPaymentIntentComCartaoTeste_deveConfirmarEDevolverResultado() throws StripeException {
        StripeGat gat = new StripeGat();

        PaymentIntent piOriginal = mock(PaymentIntent.class);
        PaymentIntent piConfirmado = mock(PaymentIntent.class);

        when(piOriginal.confirm(any(PaymentIntentConfirmParams.class)))
                .thenReturn(piConfirmado);

        try (MockedStatic<PaymentIntent> paymentIntent =
                     Mockito.mockStatic(PaymentIntent.class)) {

            paymentIntent.when(() -> PaymentIntent.retrieve("pi_test_123"))
                    .thenReturn(piOriginal);

            PaymentIntent retorno =
                    gat.confirmarPaymentIntentComCartaoTeste("pi_test_123");

            paymentIntent.verify(() -> PaymentIntent.retrieve("pi_test_123"));
            verify(piOriginal).confirm(any(PaymentIntentConfirmParams.class));
            assertSame(piConfirmado, retorno);
        }
    }
}
