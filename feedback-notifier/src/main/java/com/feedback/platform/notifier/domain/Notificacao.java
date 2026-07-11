package com.feedback.platform.notifier.domain;

import java.time.Instant;
import java.util.Objects;

public record Notificacao(
        String id,
        String feedbackId,
        String professorId,
        String alunoId,
        String urgencia,
        String email,
        String assunto,
        String corpo,
        StatusNotificacao status,
        Instant dataCriacao,
        Instant dataEnvio
) {

    public Notificacao {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(feedbackId, "feedbackId é obrigatório");
        Objects.requireNonNull(professorId, "professorId é obrigatório");
        Objects.requireNonNull(alunoId, "alunoId é obrigatório");
        Objects.requireNonNull(urgencia, "urgencia é obrigatória");
        Objects.requireNonNull(email, "email é obrigatório");
        Objects.requireNonNull(assunto, "assunto é obrigatório");
        Objects.requireNonNull(corpo, "corpo é obrigatório");
        Objects.requireNonNull(status, "status é obrigatório");
        Objects.requireNonNull(dataCriacao, "dataCriacao é obrigatória");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id não pode ser vazio");
        }
        if (feedbackId.isBlank()) {
            throw new IllegalArgumentException("feedbackId não pode ser vazio");
        }
        if (professorId.isBlank()) {
            throw new IllegalArgumentException("professorId não pode ser vazio");
        }
        if (alunoId.isBlank()) {
            throw new IllegalArgumentException("alunoId não pode ser vazio");
        }
        if (!urgencia.matches("BAIXA|MEDIA|ALTA|CRITICA")) {
            throw new IllegalArgumentException("urgencia deve ser: BAIXA, MEDIA, ALTA ou CRITICA");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("email não pode ser vazio");
        }
        if (assunto.isBlank()) {
            throw new IllegalArgumentException("assunto não pode ser vazio");
        }
        if (corpo.isBlank()) {
            throw new IllegalArgumentException("corpo não pode ser vazio");
        }
    }

    public enum StatusNotificacao {
        PENDENTE,
        ENVIADA,
        FALHA,
        CANCELADA
    }
}
