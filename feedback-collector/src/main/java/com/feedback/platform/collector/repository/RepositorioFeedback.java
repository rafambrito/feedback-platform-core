package com.feedback.platform.collector.repository;

import com.feedback.platform.collector.domain.Feedback;

public interface RepositorioFeedback {

    void salvar(Feedback feedback);

    Feedback buscarPorId(String id);
}
