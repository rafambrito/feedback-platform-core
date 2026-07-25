package com.feedback.platform.reporter.service;

import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;

public interface ServicoRelatorioFeedback {

    ProfessorReportResponseDTO obterRelatorioProfessor(String professorId);

    CursoReportResponseDTO obterRelatorioCurso(String cursoId);

    ReportSemanalResponseDTO obterRelatorioSemanalCurso(String cursoId, String professorId);
}
