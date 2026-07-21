package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.service.FeedbackReportService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/reports")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class FeedbackReporterResource {

    private final FeedbackReportService feedbackReportService;

    @Inject
    public FeedbackReporterResource(FeedbackReportService feedbackReportService) {
        this.feedbackReportService = feedbackReportService;
    }

    @GET
    @Path("/professor/{professorId}")
    public Response getProfessorReport(@PathParam("professorId") String professorId) {
        ProfessorReportResponseDTO response = feedbackReportService.getProfessorReport(professorId);

        return Response.ok(response).build();
    }

    @GET
    @Path("/curso/{cursoId}")
    public Response getCursoReport(@PathParam("cursoId") String cursoId) {
        CursoReportResponseDTO response = feedbackReportService.getCursoReport(cursoId);

        return Response.ok(response).build();
    }

    @GET
    @Path("/semanal")
    public Response getRelatorioSemanalCurso(@QueryParam("cursoId") String cursoId,
                                             @QueryParam("professorId") String professorId) {
        String normalizedCursoId = normalize(cursoId);
        String normalizedProfessorId = normalize(professorId);

        if (normalizedCursoId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "cursoId é obrigatório"))
                    .build();
        }

        try {
            ReportSemanalResponseDTO response = feedbackReportService.getRelatorioSemanalCurso(normalizedCursoId, normalizedProfessorId);

            return Response.ok(response).build();
        } catch (Exception exception) {
            return Response.serverError()
                    .entity(Map.of("message", "Erro ao gerar relatório semanal"))
                    .build();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
