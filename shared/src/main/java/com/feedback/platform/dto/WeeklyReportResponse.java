package com.feedback.platform.dto;

import java.time.Instant;

public record WeeklyReportResponse(
        String cursoId,
        int totalFeedbacks,
        Instant geradoEm
) {
}
