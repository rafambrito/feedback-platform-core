package com.feedback.platform.reporter.service.impl;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.FeedbackReportItemDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.dto.WeeklyCourseReportResponseDTO;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import com.feedback.platform.reporter.service.FeedbackReportService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public WeeklyCourseReportResponseDTO getWeeklyCourseReport(String courseId, String professorId) {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);

        List<FeedbackRecord> source = (professorId == null || professorId.isBlank())
                ? feedbackRepository.findByCursoId(courseId)
                : feedbackRepository.findByCursoIdAndProfessorId(courseId, professorId);

        List<FeedbackRecord> weekly = source.stream()
                .filter(feedback -> feedback.dataCriacao() != null)
                .filter(feedback -> !feedback.dataCriacao().isBefore(cutoff))
                .toList();

        long baixaCount = weekly.stream().filter(f -> "BAIXA".equalsIgnoreCase(f.criticidade())).count();
        long mediaCount = weekly.stream().filter(f -> "MEDIA".equalsIgnoreCase(f.criticidade())).count();
        long altaCount = weekly.stream().filter(f -> "ALTA".equalsIgnoreCase(f.criticidade())).count();

        double averageNota = weekly.stream()
                .map(FeedbackRecord::nota)
                .filter(nota -> nota != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        Map<String, Long> feedbacksByProfessor = weekly.stream()
                .collect(Collectors.groupingBy(FeedbackRecord::professorId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return new WeeklyCourseReportResponseDTO(
                courseId,
                professorId,
                weekly.size(),
                averageNota,
                baixaCount,
                mediaCount,
                altaCount,
                feedbacksByProfessor,
                Instant.now()
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
