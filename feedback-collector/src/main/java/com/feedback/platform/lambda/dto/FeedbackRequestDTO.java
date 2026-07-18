package com.feedback.platform.lambda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FeedbackRequestDTO(
        @NotBlank String cursoId,
        @NotBlank String alunoId,
        @NotBlank String professorId,
        @Min(0) @Max(10) int nota,
        @NotBlank String comentario
) {
}
