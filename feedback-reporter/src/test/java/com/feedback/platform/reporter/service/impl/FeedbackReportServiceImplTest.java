package com.feedback.platform.reporter.service.impl;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.dto.WeeklyCourseReportResponseDTO;
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

            WeeklyCourseReportResponseDTO response = feedbackReportService.getWeeklyCourseReport("curso-123", null);

            assertEquals("curso-123", response.courseId());
        assertEquals(1, response.totalFeedbacks());
            assertEquals(5.0, response.averageNota());
            assertEquals(1, response.baixaCount());
            assertEquals(0, response.mediaCount());
            assertEquals(0, response.altaCount());
            assertEquals(1L, response.feedbacksByProfessor().get("prof-1"));
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

        WeeklyCourseReportResponseDTO response = feedbackReportService.getWeeklyCourseReport("curso-123", null);

        assertEquals(0, response.totalFeedbacks());
        assertEquals(0.0, response.averageNota());
        assertTrue(response.feedbacksByProfessor().isEmpty());
    }

    @Test
    void deveRetornarZeroQuandoNaoExistemFeedbacks() {
        when(feedbackRepository.findByCursoId("curso-123")).thenReturn(List.of());

        WeeklyCourseReportResponseDTO response = feedbackReportService.getWeeklyCourseReport("curso-123", null);

        assertEquals(0, response.totalFeedbacks());
        assertTrue(response.feedbacksByProfessor().isEmpty());
    }

    @Test
    void deveAplicarFiltroPorProfessorQuandoInformado() {
        Instant now = Instant.now();
        FeedbackRecord p1a = new FeedbackRecord("f-1", "curso-123", "aluno-1", "prof-1", 4, "ok", "MEDIA", now.minusSeconds(60));
        FeedbackRecord p1b = new FeedbackRecord("f-2", "curso-123", "aluno-2", "prof-1", 2, "ruim", "ALTA", now.minusSeconds(120));

        when(feedbackRepository.findByCursoIdAndProfessorId("curso-123", "prof-1")).thenReturn(List.of(p1a, p1b));

        WeeklyCourseReportResponseDTO response = feedbackReportService.getWeeklyCourseReport("curso-123", "prof-1");

        assertEquals("prof-1", response.professorId());
        assertEquals(2, response.totalFeedbacks());
        assertEquals(3.0, response.averageNota());
        assertEquals(0, response.baixaCount());
        assertEquals(1, response.mediaCount());
        assertEquals(1, response.altaCount());
        assertEquals(2L, response.feedbacksByProfessor().get("prof-1"));
    }
}