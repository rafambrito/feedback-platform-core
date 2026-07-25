package com.feedback.platform.collector.service;

import com.feedback.platform.domain.Feedback;
import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.event.PublicadorEvento;
import com.feedback.platform.repository.RepositorioFeedback;
import com.feedback.platform.service.impl.ServicoFeedbackImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicoFeedbackImplTestColetor {

    @Test
    void createFeedback_shouldSaveAndReturnResponse() {
        List<Feedback> stored = new ArrayList<>();
        RepositorioFeedback repository = new RepositorioFeedback() {
            @Override
            public void salvar(Feedback feedback) {
                stored.add(feedback);
            }

            @Override
            public Feedback buscarPorId(String id) {
                return null;
            }
        };

        PublicadorEvento eventPublisher = new PublicadorEvento() {
            @Override
            public void publicarFeedbackCritico(String feedbackId, String alunoId, String professorId) {
            }
        };

        com.feedback.platform.service.ServicoFeedback service = new ServicoFeedbackImpl(repository, eventPublisher);

        FeedbackRequestDTO request = new FeedbackRequestDTO("course-1", "student-1", "teacher-1", 5, "Precisa de melhoria urgente");

        FeedbackResponseDTO response = service.processarFeedback(request);

        assertNotNull(response.id());
        assertEquals(1, stored.size());
        assertEquals("course-1", stored.get(0).cursoId());
    }
}
