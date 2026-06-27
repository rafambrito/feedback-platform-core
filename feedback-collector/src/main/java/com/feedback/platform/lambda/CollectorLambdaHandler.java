package com.feedback.platform.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.feedback.platform.lambda.dto.FeedbackRequestDTO;
import com.feedback.platform.lambda.repository.LambdaFeedbackRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectorLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final LambdaFeedbackRepository repository;

    public CollectorLambdaHandler() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.repository = new LambdaFeedbackRepository();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        context.getLogger().log("[" + requestId + "] Iniciando processamento de coleta de feedback");

        try {
            String body = event.getBody();
            if (body == null || body.isBlank()) {
                context.getLogger().log("[" + requestId + "] Payload vazio");
                return response(400, "Payload de entrada é obrigatório");
            }

            FeedbackRequestDTO request = objectMapper.readValue(body, FeedbackRequestDTO.class);
            Set<ConstraintViolation<FeedbackRequestDTO>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining("; "));

                context.getLogger().log("[" + requestId + "] Falha de validação: " + details);
                return response(400, details);
            }

            repository.save(request, requestId);
            context.getLogger().log("[" + requestId + "] Feedback persistido com sucesso");
            return response(201, "Feedback recebido com sucesso");

        } catch (JsonProcessingException e) {
            context.getLogger().log("[" + requestId + "] JSON inválido: " + e.getMessage());
            return response(400, "Payload JSON inválido");
        } catch (Exception e) {
            context.getLogger().log("[" + requestId + "] Erro interno: " + e.getMessage());
            return response(500, "Erro interno ao processar feedback");
        }
    }

    private APIGatewayProxyResponseEvent response(int statusCode, String message) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of("message", message));
        } catch (JsonProcessingException e) {
            body = "{\"message\":\"Erro ao serializar resposta\"}";
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}
