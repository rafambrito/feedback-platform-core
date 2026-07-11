package com.feedback.platform.reporter.dto;

import java.time.Instant;
import java.util.Map;

public record WeeklyCourseReportResponseDTO(
        String courseId,
        String professorId,
        int totalFeedbacks,
        double averageNota,
        long baixaCount,
        long mediaCount,
        long altaCount,
        Map<String, Long> feedbacksByProfessor,
        Instant generatedAt
) {
}
