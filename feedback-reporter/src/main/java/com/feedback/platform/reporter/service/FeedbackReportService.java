package com.feedback.platform.reporter.service;

import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;

public interface FeedbackReportService {

    ProfessorReportResponseDTO getProfessorReport(String professorId);

    CursoReportResponseDTO getCursoReport(String cursoId);

    CursoReportResponseDTO getWeeklyCourseReport(String courseId);
}
