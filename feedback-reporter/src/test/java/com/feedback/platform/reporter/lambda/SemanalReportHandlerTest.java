package com.feedback.platform.reporter.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.service.semanal.RelatorioSemanalSchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanalReportHandlerTest {

    @Mock
    private RelatorioSemanalSchedulerService relatorioSemanalSchedulerService;

    @Mock
    private Context context;

    @Test
    void deveGerarRelatorioSemanalComSucesso() {
        ReportSemanalResponseDTO expected = new ReportSemanalResponseDTO(
                "curso-123",
                "prof-1",
                2,
                4.5,
                0,
                1,
                1,
            Map.of("2026-07-01", 2L),
                Map.of("prof-1", 2L),
                Instant.parse("2026-07-01T10:00:00Z")
        );

        LambdaLogger logger = mock(LambdaLogger.class);
        when(context.getAwsRequestId()).thenReturn("req-1");
        when(context.getLogger()).thenReturn(logger);
        when(relatorioSemanalSchedulerService.gerarRelatorioSemanalCurso("curso-123", "prof-1")).thenReturn(expected);

        SemanalReportHandler handler = new SemanalReportHandler(relatorioSemanalSchedulerService, "curso-123", "prof-1");

        ReportSemanalResponseDTO response = handler.handleRequest(new ScheduledEvent(), context);

        assertEquals("curso-123", response.cursoId());
        assertEquals(2, response.totalFeedbacks());
        verify(relatorioSemanalSchedulerService).gerarRelatorioSemanalCurso("curso-123", "prof-1");
    }

    @Test
    void deveFalharQuandoCursoIdNaoConfigurado() {
        SemanalReportHandler handler = new SemanalReportHandler(relatorioSemanalSchedulerService, "  ", null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> handler.handleRequest(new ScheduledEvent(), context)
        );

        assertEquals("AWS_RELATORIO_SEMANAL_CURSO_ID e obrigatorio para a execucao agendada", exception.getMessage());
    }
}