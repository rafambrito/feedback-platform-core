package com.feedback.platform.notifier.dto;

import java.time.Instant;

public record NotificationResponseDTO(
        String id,
        String feedbackId,
        String professorId,
        String status,
        Instant dataCriacao,
        Instant dataEnvio
) {
}
