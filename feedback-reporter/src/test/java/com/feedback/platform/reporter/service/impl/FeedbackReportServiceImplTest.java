package com.feedback.platform.reporter.service.impl;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
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

            ReportSemanalResponseDTO response = feedbackReportService.getRelatorioSemanalCurso("curso-123", null);

            assertEquals("curso-123", response.cursoId());
        assertEquals(1, response.totalFeedbacks());
            assertEquals(5.0, response.mediaNota());
            assertEquals(1, response.quantidadeBaixa());
            assertEquals(0, response.quantidadeMedia());
            assertEquals(0, response.quantidadeAlta());
            assertEquals(1L, response.feedbacksPorProfessor().get("prof-1"));
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

        ReportSemanalResponseDTO response = feedbackReportService.getRelatorioSemanalCurso("curso-123", null);

        assertEquals(0, response.totalFeedbacks());
        assertEquals(0.0, response.mediaNota());
        assertTrue(response.feedbacksPorProfessor().isEmpty());
    }

    @Test
    void deveRetornarZeroQuandoNaoExistemFeedbacks() {
        when(feedbackRepository.findByCursoId("curso-123")).thenReturn(List.of());

        ReportSemanalResponseDTO response = feedbackReportService.getRelatorioSemanalCurso("curso-123", null);

        assertEquals(0, response.totalFeedbacks());
        assertTrue(response.feedbacksPorProfessor().isEmpty());
    }

    @Test
    void deveAplicarFiltroPorProfessorQuandoInformado() {
        Instant now = Instant.now();
        FeedbackRecord p1a = new FeedbackRecord("f-1", "curso-123", "aluno-1", "prof-1", 4, "ok", "MEDIA", now.minusSeconds(60));
        FeedbackRecord p1b = new FeedbackRecord("f-2", "curso-123", "aluno-2", "prof-1", 2, "ruim", "ALTA", now.minusSeconds(120));

        when(feedbackRepository.findByCursoIdAndProfessorId("curso-123", "prof-1")).thenReturn(List.of(p1a, p1b));

        ReportSemanalResponseDTO response = feedbackReportService.getRelatorioSemanalCurso("curso-123", "prof-1");

        assertEquals("prof-1", response.professorId());
        assertEquals(2, response.totalFeedbacks());
        assertEquals(3.0, response.mediaNota());
        assertEquals(0, response.quantidadeBaixa());
        assertEquals(1, response.quantidadeMedia());
        assertEquals(1, response.quantidadeAlta());
        assertEquals(2L, response.feedbacksPorProfessor().get("prof-1"));
    }
}