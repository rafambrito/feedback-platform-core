package com.feedback.platform.reporter.service;

import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;

public interface FeedbackReportService {

    ProfessorReportResponseDTO getProfessorReport(String professorId);

    CursoReportResponseDTO getCursoReport(String cursoId);

    ReportSemanalResponseDTO getRelatorioSemanalCurso(String cursoId, String professorId);
}
