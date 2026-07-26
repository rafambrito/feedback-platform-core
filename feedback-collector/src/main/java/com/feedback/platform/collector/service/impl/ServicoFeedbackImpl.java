package com.feedback.platform.collector.service.impl;

import com.feedback.platform.domain.Criticidade;
import com.feedback.platform.collector.domain.Feedback;
import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.collector.event.PublicadorEvento;
import com.feedback.platform.collector.repository.RepositorioFeedback;
import com.feedback.platform.collector.service.ServicoFeedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class ServicoFeedbackImpl implements ServicoFeedback {

    private final RepositorioFeedback repositorio;
    private final PublicadorEvento publicadorEvento;

    @Inject
    public ServicoFeedbackImpl(RepositorioFeedback repositorio, PublicadorEvento publicadorEvento) {
        this.repositorio = repositorio;
        this.publicadorEvento = publicadorEvento;
    }

    @Override
    public FeedbackResponseDTO processarFeedback(FeedbackRequestDTO request) {
        Criticidade criticidade = avaliarCriticidade(request);

        String id = UUID.randomUUID().toString();
        Instant dataCriacao = Instant.now();

        Feedback feedback = new Feedback(
                id,
                request.cursoId(),
                request.alunoId(),
                request.professorId(),
                request.nota(),
                request.comentario(),
                criticidade,
                dataCriacao
        );

        repositorio.salvar(feedback);

    if (criticidade == Criticidade.ALTA) {
        publicadorEvento.publicarFeedbackCritico(
            feedback.id(),
            feedback.alunoId(),
            feedback.professorId()
        );
    }

        return new FeedbackResponseDTO(
                feedback.id(),
                feedback.cursoId(),
                feedback.alunoId(),
                feedback.professorId(),
                feedback.nota(),
                feedback.comentario(),
                feedback.criticidade(),
                feedback.dataCriacao()
        );
    }

    @Override
    public Criticidade avaliarCriticidade(FeedbackRequestDTO request) {
        int nota = request.nota();
        if (nota < 3) return Criticidade.ALTA;
        if (nota <= 6) return Criticidade.MEDIA;
        return Criticidade.BAIXA;
    }

    @Override
    public FeedbackResponseDTO buscarPorId(String id) {
        var feedback = repositorio.buscarPorId(id);
        if (feedback == null) return null;

        return new FeedbackResponseDTO(
                feedback.id(),
                feedback.cursoId(),
                feedback.alunoId(),
                feedback.professorId(),
                feedback.nota(),
                feedback.comentario(),
                feedback.criticidade(),
                feedback.dataCriacao()
        );
    }
}
