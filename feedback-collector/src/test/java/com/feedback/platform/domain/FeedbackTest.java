package com.feedback.platform.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {

    @Test
    void shouldCreateValidFeedback() {
        Feedback feedback = new Feedback(
                "1",
                "curso-1",
                "aluno-1",
                "professor-1",
                8,
                "Bom curso",
                Criticidade.MEDIA,
                Instant.now());

        assertEquals("1", feedback.id());
        assertEquals(8, feedback.nota());
        assertEquals(Criticidade.MEDIA, feedback.criticidade());
    }

    @Test
    void shouldRejectInvalidNota() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Feedback(
                        "1",
                        "curso-1",
                        "aluno-1",
                        "professor-1",
                        11,
                        "Muito bom",
                        Criticidade.ALTA,
                        Instant.now()));

        assertEquals("nota não pode ser menor que 0 ou maior que 10", exception.getMessage());
    }

    @Test
    void shouldRejectBlankComentario() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Feedback(
                        "1",
                        "curso-1",
                        "aluno-1",
                        "professor-1",
                        7,
                        " ",
                        Criticidade.BAIXA,
                        Instant.now()));

        assertEquals("comentario não pode ser vazio", exception.getMessage());
    }
}
