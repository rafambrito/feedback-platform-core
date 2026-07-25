package com.feedback.platform.repository;

import com.feedback.platform.domain.Feedback;

public interface RepositorioFeedback {

    void salvar(Feedback feedback);

    Feedback buscarPorId(String id);
}
