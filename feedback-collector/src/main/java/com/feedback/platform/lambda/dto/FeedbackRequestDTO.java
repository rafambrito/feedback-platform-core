package com.feedback.platform.lambda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record FeedbackRequestDTO(
        @NotBlank @Size(max = 100) String professorId,
        @NotBlank @Size(max = 100) String cursoId,
        @NotNull @Min(1) @Max(5) Integer nota,
        @NotBlank @Size(min = 3, max = 1000) String comentario,
        @NotBlank @Pattern(regexp = "BAIXA|MEDIA|ALTA|CRITICA") String urgencia,
        @NotNull Instant timestamp
) {
}
