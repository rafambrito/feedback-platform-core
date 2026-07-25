package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.ProfessorReportResponseDTO;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.service.ServicoRelatorioFeedback;
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
public class RecursoRelatorioFeedback {

    private final ServicoRelatorioFeedback servicoRelatorioFeedback;

    @Inject
    public RecursoRelatorioFeedback(ServicoRelatorioFeedback servicoRelatorioFeedback) {
        this.servicoRelatorioFeedback = servicoRelatorioFeedback;
    }

    @GET
    @Path("/professor/{professorId}")
    public Response obterRelatorioProfessor(@PathParam("professorId") String professorId) {
        ProfessorReportResponseDTO response = servicoRelatorioFeedback.obterRelatorioProfessor(professorId);

        return Response.ok(response).build();
    }

    @GET
    @Path("/curso/{cursoId}")
    public Response obterRelatorioCurso(@PathParam("cursoId") String cursoId) {
        CursoReportResponseDTO response = servicoRelatorioFeedback.obterRelatorioCurso(cursoId);

        return Response.ok(response).build();
    }

    @GET
    @Path("/semanal")
    public Response obterRelatorioSemanalCurso(@QueryParam("cursoId") String cursoId,
                                             @QueryParam("professorId") String professorId) {
        String cursoIdNormalizado = normalizar(cursoId);
        String professorIdNormalizado = normalizar(professorId);

        if (cursoIdNormalizado == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "cursoId é obrigatório"))
                    .build();
        }

        try {
            ReportSemanalResponseDTO response = servicoRelatorioFeedback.obterRelatorioSemanalCurso(cursoIdNormalizado, professorIdNormalizado);

            return Response.ok(response).build();
        } catch (Exception exception) {
            return Response.serverError()
                    .entity(Map.of("message", "Erro ao gerar relatório semanal"))
                    .build();
        }
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }

        String valorNormalizado = value.trim();
        return valorNormalizado.isBlank() ? null : valorNormalizado;
    }
}
