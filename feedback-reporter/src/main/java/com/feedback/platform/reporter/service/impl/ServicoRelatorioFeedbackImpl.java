package com.feedback.platform.reporter.service.impl;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.FeedbackReportItemDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.repository.RepositorioFeedback;
import com.feedback.platform.reporter.service.ServicoRelatorioFeedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ServicoRelatorioFeedbackImpl implements ServicoRelatorioFeedback {

    private final RepositorioFeedback feedbackRepository;

    @Inject
    public ServicoRelatorioFeedbackImpl(RepositorioFeedback feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public ProfessorReportResponseDTO obterRelatorioProfessor(String professorId) {
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
    public CursoReportResponseDTO obterRelatorioCurso(String cursoId) {
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
    public ReportSemanalResponseDTO obterRelatorioSemanalCurso(String cursoId, String professorId) {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);

        List<FeedbackRecord> source = (professorId == null || professorId.isBlank())
                ? feedbackRepository.findByCursoId(cursoId)
                : feedbackRepository.findByCursoIdAndProfessorId(cursoId, professorId);

        List<FeedbackRecord> semanaAtual = source.stream()
                .filter(feedback -> feedback.dataCriacao() != null)
                .filter(feedback -> !feedback.dataCriacao().isBefore(cutoff))
                .toList();

        long baixaCount = semanaAtual.stream().filter(f -> "BAIXA".equalsIgnoreCase(f.criticidade())).count();
        long mediaCount = semanaAtual.stream().filter(f -> "MEDIA".equalsIgnoreCase(f.criticidade())).count();
        long altaCount = semanaAtual.stream().filter(f -> "ALTA".equalsIgnoreCase(f.criticidade())).count();

        Map<String, Long> quantidadePorDia = semanaAtual.stream()
                .collect(Collectors.groupingBy(
                        feedback -> feedback.dataCriacao().atOffset(ZoneOffset.UTC).toLocalDate().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        double averageNota = semanaAtual.stream()
                .map(FeedbackRecord::nota)
                .filter(nota -> nota != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        Map<String, Long> feedbacksByProfessor = semanaAtual.stream()
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

        return new ReportSemanalResponseDTO(
                cursoId,
                professorId,
                semanaAtual.size(),
                averageNota,
                baixaCount,
                mediaCount,
                altaCount,
                                quantidadePorDia,
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
