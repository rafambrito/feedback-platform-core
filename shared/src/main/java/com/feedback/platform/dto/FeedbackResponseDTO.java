package com.feedback.platform.dto;

import com.feedback.platform.domain.Criticidade;
import java.time.Instant;

public record FeedbackResponseDTO(
        String id,
        String cursoId,
        String alunoId,
        String professorId,
        int nota,
        String comentario,
        Criticidade criticidade,
        Instant dataCriacao
) {
}
