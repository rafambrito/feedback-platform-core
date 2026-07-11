package com.feedback.platform.notifier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FeedbackEventDTO(
        @NotBlank(message = "feedbackId é obrigatório")
        @Size(max = 100, message = "feedbackId não pode exceder 100 caracteres")
        String feedbackId,
        
        @NotBlank(message = "alunoId é obrigatório")
        @Size(max = 100, message = "alunoId não pode exceder 100 caracteres")
        String alunoId,
        
        @NotBlank(message = "professorId é obrigatório")
        @Size(max = 100, message = "professorId não pode exceder 100 caracteres")
        String professorId,
        
        @NotBlank(message = "urgencia é obrigatória")
        @Pattern(
                regexp = "BAIXA|MEDIA|ALTA|CRITICA",
                message = "urgencia deve ser: BAIXA, MEDIA, ALTA ou CRITICA"
        )
        String urgencia
) {
}
