package com.feedback.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AvaliacaoRequestDTO(
        @NotBlank String descricao,
        @NotNull @Min(0) @Max(10) Integer nota
) {
}