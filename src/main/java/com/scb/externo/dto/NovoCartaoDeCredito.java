package com.scb.externo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.YearMonth;

public record NovoCartaoDeCredito(
        String nomeTitular,
        String numero,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
        YearMonth validade,
        String cvv
) {}