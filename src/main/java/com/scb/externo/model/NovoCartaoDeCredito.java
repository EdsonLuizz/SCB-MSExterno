package com.scb.externo.model;

import jakarta.validation.constraints.NotBlank;

public record NovoCartaoDeCredito(
    @NotBlank String numero,
    @NotBlank String nome,
    @NotBlank String validade,
    @NotBlank String cvv
) {}
