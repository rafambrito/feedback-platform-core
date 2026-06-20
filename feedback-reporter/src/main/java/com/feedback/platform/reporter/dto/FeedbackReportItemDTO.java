package com.feedback.platform.reporter.dto;

import java.time.Instant;

public record FeedbackReportItemDTO(
        String id,
        String cursoId,
        String alunoId,
        String professorId,
        Integer nota,
        String comentario,
        String criticidade,
        Instant dataCriacao
) {
}
