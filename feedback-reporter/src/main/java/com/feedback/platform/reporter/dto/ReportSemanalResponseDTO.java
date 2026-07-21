package com.feedback.platform.reporter.dto;

import java.time.Instant;
import java.util.Map;

public record ReportSemanalResponseDTO(
        String cursoId,
        String professorId,
        int totalFeedbacks,
        double mediaNota,
        long quantidadeBaixa,
        long quantidadeMedia,
        long quantidadeAlta,
        Map<String, Long> quantidadePorDia,
        Map<String, Long> feedbacksPorProfessor,
        Instant geradoEm
) {
}
