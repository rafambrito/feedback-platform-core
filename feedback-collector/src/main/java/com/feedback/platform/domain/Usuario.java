package com.feedback.platform.domain;

import java.util.Objects;

public record Usuario(String id, String nome, String email, TipoUsuario tipoUsuario) {

    public Usuario {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(nome, "nome é obrigatório");
        Objects.requireNonNull(email, "email é obrigatório");
        Objects.requireNonNull(tipoUsuario, "tipoUsuario é obrigatório");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id não pode ser vazio");
        }
        if (nome.isBlank()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("email não pode ser vazio");
        }
    }
}
