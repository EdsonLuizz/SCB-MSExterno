package com.scb.externo.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NovaCobranca(
    @NotBlank String ciclista,
    @NotNull @Min(1) Long valor
) {}
