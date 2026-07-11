package com.feedback.platform.reporter.service.impl;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.FeedbackReportItemDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import com.feedback.platform.reporter.service.FeedbackReportService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class FeedbackReportServiceImpl implements FeedbackReportService {

    private final FeedbackRepository feedbackRepository;

    @Inject
    public FeedbackReportServiceImpl(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public ProfessorReportResponseDTO getProfessorReport(String professorId) {
        List<FeedbackReportItemDTO> items = feedbackRepository.findByProfessorId(professorId).stream()
                .map(this::toDto)
                .toList();

        return new ProfessorReportResponseDTO(
                professorId,
                items.size(),
                items
        );
    }

    @Override
    public CursoReportResponseDTO getCursoReport(String cursoId) {
        List<FeedbackReportItemDTO> items = feedbackRepository.findByCursoId(cursoId).stream()
                .map(this::toDto)
                .toList();

        return new CursoReportResponseDTO(
                cursoId,
                items.size(),
                items
        );
    }

    @Override
    public CursoReportResponseDTO getWeeklyCourseReport(String courseId) {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);

        List<FeedbackReportItemDTO> items = feedbackRepository.findByCursoId(courseId).stream()
                .filter(feedback -> feedback.dataCriacao() != null)
                .filter(feedback -> !feedback.dataCriacao().isBefore(cutoff))
                .map(this::toDto)
                .toList();

        return new CursoReportResponseDTO(
                courseId,
                items.size(),
                items
        );
    }

    private FeedbackReportItemDTO toDto(FeedbackRecord feedback) {
        return new FeedbackReportItemDTO(
                feedback.id(),
                feedback.cursoId(),
                feedback.alunoId(),
                feedback.professorId(),
                feedback.nota(),
                feedback.comentario(),
                feedback.criticidade(),
                feedback.dataCriacao()
        );
    }
}
