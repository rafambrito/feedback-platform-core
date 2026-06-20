package com.feedback.platform.notifier.dto;

public record FeedbackEventDTO(
        String feedbackId,
        String alunoId,
        String professorId
) {
}
