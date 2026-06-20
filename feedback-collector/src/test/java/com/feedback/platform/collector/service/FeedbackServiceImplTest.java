package com.feedback.platform.collector.service;

import com.feedback.platform.domain.Feedback;
import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.event.EventPublisher;
import com.feedback.platform.repository.FeedbackRepository;
import com.feedback.platform.service.impl.FeedbackServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackServiceImplTest {

    @Test
    void createFeedback_shouldSaveAndReturnResponse() {
        List<Feedback> stored = new ArrayList<>();
        FeedbackRepository repository = new FeedbackRepository() {
            @Override
            public void save(Feedback feedback) {
                stored.add(feedback);
            }

            @Override
            public Feedback findById(String id) {
                return null;
            }
        };

        EventPublisher eventPublisher = new EventPublisher() {
            @Override
            public void publishCriticalFeedback(String feedbackId, String alunoId, String professorId) {
            }
        };

        com.feedback.platform.service.FeedbackService service = new FeedbackServiceImpl(repository, eventPublisher);

        FeedbackRequestDTO request = new FeedbackRequestDTO("course-1", "student-1", "teacher-1", 5, "Precisa de melhoria urgente");

        FeedbackResponseDTO response = service.processarFeedback(request);

        assertNotNull(response.id());
        assertEquals(1, stored.size());
        assertEquals("course-1", stored.get(0).cursoId());
    }
}
