package com.feedback.platform.service.impl;

import com.feedback.platform.domain.Criticidade;
import com.feedback.platform.domain.Feedback;
import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.event.EventPublisher;
import com.feedback.platform.repository.FeedbackRepository;
import com.feedback.platform.service.FeedbackService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository repository;
    private final EventPublisher eventPublisher;

    @Inject
    public FeedbackServiceImpl(FeedbackRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
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

        repository.save(feedback);

    if (criticidade == Criticidade.ALTA) {
        eventPublisher.publishCriticalFeedback(
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
        var feedback = repository.findById(id);
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
