package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.FeedbackReportItemDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/reports")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class FeedbackReporterResource {

    private final FeedbackRepository feedbackRepository;

    @Inject
    public FeedbackReporterResource(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @GET
    @Path("/professor/{professorId}")
    public Response getProfessorReport(@PathParam("professorId") String professorId) {
        List<FeedbackRecord> feedbacks = feedbackRepository.findByProfessorId(professorId);
        List<FeedbackReportItemDTO> items = feedbacks.stream().map(this::toDto).toList();

        ProfessorReportResponseDTO response = new ProfessorReportResponseDTO(
                professorId,
                items.size(),
                items
        );

        return Response.ok(response).build();
    }

    @GET
    @Path("/curso/{cursoId}")
    public Response getCursoReport(@PathParam("cursoId") String cursoId) {
        List<FeedbackRecord> feedbacks = feedbackRepository.findByCursoId(cursoId);
        List<FeedbackReportItemDTO> items = feedbacks.stream().map(this::toDto).toList();

        CursoReportResponseDTO response = new CursoReportResponseDTO(
                cursoId,
                items.size(),
                items
        );

        return Response.ok(response).build();
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
