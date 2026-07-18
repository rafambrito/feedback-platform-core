package com.feedback.platform.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.feedback.platform.lambda.repository.LambdaFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorLambdaHandlerTest {

    @Mock
    private LambdaFeedbackRepository repository;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private CollectorLambdaHandler handler;

    @BeforeEach
    void setUp() {
        when(context.getAwsRequestId()).thenReturn("req-1");
        when(context.getLogger()).thenReturn(logger);
        handler = new CollectorLambdaHandler(repository);
    }

    @Test
    void getFeedbackByIdWithoutBodyReturns200() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/feedback/272ea304-b9f6-45f2-b724-5e8a77b753e0")
                .withPathParameters(Map.of("id", "272ea304-b9f6-45f2-b724-5e8a77b753e0"));

        when(repository.findById("272ea304-b9f6-45f2-b724-5e8a77b753e0")).thenReturn(Map.of(
                "id", AttributeValue.builder().s("272ea304-b9f6-45f2-b724-5e8a77b753e0").build(),
                "cursoId", AttributeValue.builder().s("1TIA").build(),
                "alunoId", AttributeValue.builder().s("aluno-001").build(),
                "professorId", AttributeValue.builder().s("prof-001").build(),
                "nota", AttributeValue.builder().n("8").build(),
                "comentario", AttributeValue.builder().s("Aula clara").build(),
                "criticidade", AttributeValue.builder().s("BAIXA").build(),
                "dataCriacao", AttributeValue.builder().s("2026-07-18T00:00:00Z").build()
        ));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"id\":\"272ea304-b9f6-45f2-b724-5e8a77b753e0\""));
        assertTrue(response.getBody().contains("\"nota\":8"));
    }

    @Test
    void getFeedbackByIdNotFoundReturns404() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPathParameters(Map.of("id", "nao-existe"));

        when(repository.findById("nao-existe")).thenReturn(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Feedback nao encontrado"));
    }

    @Test
    void postWithoutPayloadReturns400() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody("");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Payload de entrada é obrigatório"));
    }
}
