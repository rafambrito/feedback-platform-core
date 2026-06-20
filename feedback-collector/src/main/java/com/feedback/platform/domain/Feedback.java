package com.feedback.platform.domain;

import java.time.Instant;
import java.util.Objects;

public record Feedback(
        String id,
        String cursoId,
        String alunoId,
        String professorId,
        int nota,
        String comentario,
        Criticidade criticidade,
        Instant dataCriacao) {

    public Feedback {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(cursoId, "cursoId é obrigatório");
        Objects.requireNonNull(alunoId, "alunoId é obrigatório");
        Objects.requireNonNull(professorId, "professorId é obrigatório");
        Objects.requireNonNull(comentario, "comentario é obrigatório");
        Objects.requireNonNull(criticidade, "criticidade é obrigatória");
        Objects.requireNonNull(dataCriacao, "dataCriacao é obrigatória");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id não pode ser vazio");
        }
        if (cursoId.isBlank()) {
            throw new IllegalArgumentException("cursoId não pode ser vazio");
        }
        if (alunoId.isBlank()) {
            throw new IllegalArgumentException("alunoId não pode ser vazio");
        }
        if (professorId.isBlank()) {
            throw new IllegalArgumentException("professorId não pode ser vazio");
        }
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("nota não pode ser menor que 0 ou maior que 10");
        }
        if (comentario.isBlank()) {
            throw new IllegalArgumentException("comentario não pode ser vazio");
        }
    }
}
