package com.feedback.platform.notifier.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifierLambdaHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private NotifierLambdaHandler handler;

    @BeforeEach
    void setUp() {
        when(context.getAwsRequestId()).thenReturn("req-1");
        when(context.getLogger()).thenReturn(logger);
        handler = new NotifierLambdaHandler(notificationService);
    }

    @Test
    void postUrgentReturns201() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withPath("/notifications/urgent")
                .withBody("{\"feedbackId\":\"fb-1\",\"alunoId\":\"aluno-1\",\"professorId\":\"prof-1\",\"urgencia\":\"CRITICA\"}");

        Notificacao notificacao = new Notificacao(
                "notif-1",
                "fb-1",
                "prof-1",
                "aluno-1",
                "CRITICA",
                "prof-1@universidade.edu.br",
                "Assunto",
                "Corpo",
                Notificacao.StatusNotificacao.PENDENTE,
                Instant.now(),
                null
        );

        when(notificationService.processarNotificacao(any())).thenReturn(notificacao);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(201, response.getStatusCode());
        assertTrue(response.getBody().contains("\"id\":\"notif-1\""));
        assertTrue(response.getBody().contains("\"feedbackId\":\"fb-1\""));
    }

    @Test
    void getByIdReturns404WhenNotFound() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/notifications/not-found")
                .withPathParameters(Map.of("id", "not-found"));

        when(notificationService.buscarPorId("not-found")).thenReturn(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Notificacao nao encontrada"));
    }

    @Test
    void simulateReturns200() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withPath("/notifications/test/simulate")
                .withBody("{\"feedbackId\":\"f-1\",\"alunoId\":\"a-1\",\"professorId\":\"p-1\",\"urgencia\":\"ALTA\"}");

        doNothing().when(notificationService).simularRecebimento(any());

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Simulacao processada com sucesso"));
    }
}