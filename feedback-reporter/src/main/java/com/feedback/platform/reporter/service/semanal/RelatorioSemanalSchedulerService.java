package com.feedback.platform.reporter.service.semanal;

import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.service.ServicoRelatorioFeedback;

public class RelatorioSemanalSchedulerService {

    private final ServicoRelatorioFeedback feedbackReportService;

    public RelatorioSemanalSchedulerService(ServicoRelatorioFeedback feedbackReportService) {
        this.feedbackReportService = feedbackReportService;
    }

    public ReportSemanalResponseDTO gerarRelatorioSemanalCurso(String cursoId, String professorId) {
        return feedbackReportService.obterRelatorioSemanalCurso(cursoId, professorId);
    }
}