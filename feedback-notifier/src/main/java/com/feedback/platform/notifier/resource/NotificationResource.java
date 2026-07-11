package com.feedback.platform.notifier.resource;

import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.dto.FeedbackEventDTO;
import com.feedback.platform.notifier.dto.NotificationResponseDTO;
import com.feedback.platform.notifier.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notificações", description = "Endpoints para gerenciar notificações")
public class NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResource.class);

    @Inject
    private NotificationService notificationService;

    @POST
    @Path("/urgent")
    @Operation(summary = "Enviar notificação urgente", description = "Processa evento de feedback urgente e envia notificação ao professor")
    @APIResponse(responseCode = "201", description = "Notificação criada com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NotificationResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "500", description = "Erro interno no servidor")
    public Response enviarNotificacaoUrgente(@Valid FeedbackEventDTO event) {
        try {
            LOG.infof("POST /notifications/urgent - Recebido evento para professor: %s", event.professorId());
            
            Notificacao notificacao = notificationService.procesarNotificacao(event);
            NotificationResponseDTO response = mapearParaResponse(notificacao);
            
            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
        } catch (IllegalArgumentException e) {
            LOG.warnf("Validação falhou: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Erro ao processar notificação urgente");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Erro ao processar notificação"))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar notificação", description = "Retorna detalhes de uma notificação pelo ID")
    @APIResponse(responseCode = "200", description = "Notificação encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NotificationResponseDTO.class)))
    @APIResponse(responseCode = "404", description = "Notificação não encontrada")
    public Response buscarNotificacao(@PathParam("id") String id) {
        try {
            LOG.infof("GET /notifications/%s", id);
            
            Notificacao notificacao = notificationService.buscarPorId(id);
            if (notificacao == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            NotificationResponseDTO response = mapearParaResponse(notificacao);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.errorf(e, "Erro ao buscar notificação %s", id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Erro ao buscar notificação"))
                    .build();
        }
    }

    @POST
    @Path("/test/simulate")
    @Operation(summary = "Simular recebimento de mensagem", description = "Endpoint para testes - simula recebimento de evento SQS")
    @APIResponse(responseCode = "200", description = "Simulação processada com sucesso")
    @APIResponse(responseCode = "400", description = "Mensagem inválida")
    public Response simularRecebimento(String messageBody) {
        try {
            LOG.infof("POST /notifications/test/simulate - Simulando recebimento");
            notificationService.simularRecebimento(messageBody);
            return Response.ok(Map.of("message", "Simulação processada com sucesso")).build();
        } catch (Exception e) {
            LOG.errorf(e, "Erro ao simular recebimento");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private NotificationResponseDTO mapearParaResponse(Notificacao notificacao) {
        return new NotificationResponseDTO(
                notificacao.id(),
                notificacao.feedbackId(),
                notificacao.professorId(),
                notificacao.status().name(),
                notificacao.dataCriacao(),
                notificacao.dataEnvio()
        );
    }

    // Simples mapa para respostas de erro
    private static class Map {
        static java.util.Map<String, Object> of(String key, Object value) {
            var map = new java.util.HashMap<String, Object>();
            map.put(key, value);
            return map;
        }
    }
}
