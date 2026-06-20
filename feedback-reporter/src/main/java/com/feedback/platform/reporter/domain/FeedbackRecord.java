package com.feedback.platform.reporter.domain;

import java.time.Instant;

public record FeedbackRecord(
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
