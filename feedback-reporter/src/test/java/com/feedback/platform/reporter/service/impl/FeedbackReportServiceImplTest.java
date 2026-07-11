package com.feedback.platform.reporter.service.impl;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackReportServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackReportServiceImpl feedbackReportService;

    @Test
    void deveFiltrarApenasFeedbacksDaUltimaSemana() {
        Instant now = Instant.now();
        FeedbackRecord recente = new FeedbackRecord(
                "f-1",
                "curso-123",
                "aluno-1",
                "prof-1",
                5,
                "Bom curso",
                "BAIXA",
                now.minusSeconds(2 * 24 * 60 * 60)
        );
        FeedbackRecord antigo = new FeedbackRecord(
                "f-2",
                "curso-123",
                "aluno-2",
                "prof-2",
                3,
                "Conteúdo desatualizado",
                "MEDIA",
                now.minusSeconds(8 * 24 * 60 * 60)
        );

        when(feedbackRepository.findByCursoId("curso-123")).thenReturn(List.of(recente, antigo));

        CursoReportResponseDTO response = feedbackReportService.getWeeklyCourseReport("curso-123");

        assertEquals("curso-123", response.cursoId());
        assertEquals(1, response.totalFeedbacks());
        assertEquals(1, response.feedbacks().size());
        assertEquals("f-1", response.feedbacks().getFirst().id());
    }

    @Test
    void deveRetornarTotalZeroQuandoNaoHaDadosRecentes() {
        Instant now = Instant.now();
        FeedbackRecord antigo = new FeedbackRecord(
                "f-2",
                "curso-123",
                "aluno-2",
                "prof-2",
                3,
                "Conteúdo desatualizado",
                "MEDIA",
                now.minusSeconds(10 * 24 * 60 * 60)
        );

        when(feedbackRepository.findByCursoId("curso-123")).thenReturn(List.of(antigo));

        CursoReportResponseDTO response = feedbackReportService.getWeeklyCourseReport("curso-123");

        assertEquals(0, response.totalFeedbacks());
        assertTrue(response.feedbacks().isEmpty());
    }

    @Test
    void deveRetornarZeroQuandoNaoExistemFeedbacks() {
        when(feedbackRepository.findByCursoId("curso-123")).thenReturn(List.of());

        CursoReportResponseDTO response = feedbackReportService.getWeeklyCourseReport("curso-123");

        assertEquals(0, response.totalFeedbacks());
        assertTrue(response.feedbacks().isEmpty());
    }
}