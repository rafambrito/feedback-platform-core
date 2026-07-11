package com.feedback.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UrgencyNotification(
        @NotBlank String feedbackId,
        @NotBlank String alunoId,
        @NotBlank String professorId,
        @NotBlank @Pattern(regexp = "BAIXA|MEDIA|ALTA|CRITICA") String urgencia
) {
}
