package com.feedback.platform.resource;

import com.feedback.platform.dto.AvaliacaoRequestDTO;
import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.service.FeedbackService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/avaliacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class AvaliacaoResource {

    private static final String DEFAULT_CURSO_ID = "CURSO-GERAL";
    private static final String DEFAULT_ALUNO_ID = "ALUNO-ANONIMO";
    private static final String DEFAULT_PROFESSOR_ID = "PROF-GERAL";

    private final FeedbackService feedbackService;

    @Inject
    public AvaliacaoResource(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @POST
    public Response createAvaliacao(@Valid AvaliacaoRequestDTO request) {
        FeedbackRequestDTO feedbackRequest = new FeedbackRequestDTO(
                DEFAULT_CURSO_ID,
                DEFAULT_ALUNO_ID,
                DEFAULT_PROFESSOR_ID,
                request.nota(),
                request.descricao()
        );

        FeedbackResponseDTO response = feedbackService.processarFeedback(feedbackRequest);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}