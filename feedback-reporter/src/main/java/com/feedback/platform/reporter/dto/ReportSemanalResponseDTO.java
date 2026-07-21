package com.feedback.platform.reporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
                @JsonProperty("feedbacksByDay")
        Map<String, Long> quantidadePorDia,
                @JsonProperty("feedbacksByProfessor")
        Map<String, Long> feedbacksPorProfessor,
        Instant geradoEm
) {
        @JsonProperty("quantidadePorDia")
        public Map<String, Long> quantidadePorDiaLegacy() {
                return quantidadePorDia;
        }

        @JsonProperty("feedbacksPorProfessor")
        public Map<String, Long> feedbacksPorProfessorLegacy() {
                return feedbacksPorProfessor;
        }
}
