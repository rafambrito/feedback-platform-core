package com.feedback.platform.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void shouldCreateValidUsuario() {
        Usuario usuario = new Usuario("1", "Joao", "joao@example.com", TipoUsuario.ALUNO);

        assertEquals("1", usuario.id());
        assertEquals(TipoUsuario.ALUNO, usuario.tipoUsuario());
    }

    @Test
    void shouldRejectBlankNome() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Usuario("1", " ", "joao@example.com", TipoUsuario.PROFESSOR));

        assertEquals("nome não pode ser vazio", exception.getMessage());
    }
}
