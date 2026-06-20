package com.feedback.platform.reporter.dto;

import java.util.List;

public record ProfessorReportResponseDTO(
        String professorId,
        int totalFeedbacks,
        List<FeedbackReportItemDTO> feedbacks
) {
}
