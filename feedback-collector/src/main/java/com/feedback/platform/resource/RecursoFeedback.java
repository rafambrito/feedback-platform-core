package com.feedback.platform.resource;

import com.feedback.platform.dto.FeedbackRequestDTO;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.service.ServicoFeedback;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class RecursoFeedback {

    private final ServicoFeedback feedbackService;

    @Inject
    public RecursoFeedback(ServicoFeedback feedbackService) {
        this.feedbackService = feedbackService;
    }

    @POST
    public Response criarFeedback(@Valid FeedbackRequestDTO request) {
        FeedbackResponseDTO response = feedbackService.processarFeedback(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("{id}")
    public Response obterFeedback(@PathParam("id") String id) {
        FeedbackResponseDTO response = feedbackService.buscarPorId(id);
        if (response == null) {
            var payload = java.util.Map.of(
                    "error_code", "NOT_FOUND",
                    "message", "Feedback nao encontrado"
            );
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(payload)
                    .build();
        }
        return Response.ok(response).build();
    }
}
