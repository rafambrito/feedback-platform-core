package com.feedback.platform.reporter.dto;

import java.util.List;

public record CursoReportResponseDTO(
        String cursoId,
        int totalFeedbacks,
        List<FeedbackReportItemDTO> feedbacks
) {
}
