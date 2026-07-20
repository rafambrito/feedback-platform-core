package com.feedback.platform.reporter.service.semanal;

import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.service.FeedbackReportService;

public class RelatorioSemanalSchedulerService {

    private final FeedbackReportService feedbackReportService;

    public RelatorioSemanalSchedulerService(FeedbackReportService feedbackReportService) {
        this.feedbackReportService = feedbackReportService;
    }

    public ReportSemanalResponseDTO gerarRelatorioSemanalCurso(String cursoId, String professorId) {
        return feedbackReportService.getRelatorioSemanalCurso(cursoId, professorId);
    }
}