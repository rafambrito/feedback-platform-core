package com.feedback.platform.reporter.service;

import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.dto.WeeklyCourseReportResponseDTO;

public interface FeedbackReportService {

    ProfessorReportResponseDTO getProfessorReport(String professorId);

    CursoReportResponseDTO getCursoReport(String cursoId);

    WeeklyCourseReportResponseDTO getWeeklyCourseReport(String courseId, String professorId);
}
