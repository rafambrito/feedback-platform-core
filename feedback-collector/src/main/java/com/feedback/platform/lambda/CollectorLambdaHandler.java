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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectorLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "https://rafambrito.github.io",
            "Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token"
    );

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

    CollectorLambdaHandler(LambdaFeedbackRepository repository) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        String method = event.getHttpMethod() == null ? "" : event.getHttpMethod().toUpperCase();

        context.getLogger().log("[" + requestId + "] Iniciando processamento de coleta de feedback. method=" + method);

        try {
            if ("OPTIONS".equals(method)) {
                return emptyResponse(204);
            }

            if ("GET".equals(method)) {
                return handleGetById(event, requestId, context);
            }

            if (!"POST".equals(method)) {
                return response(405, "Metodo nao suportado");
            }

            String body = resolveBody(event);
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

    private APIGatewayProxyResponseEvent handleGetById(APIGatewayProxyRequestEvent event,
                                                        String requestId,
                                                        Context context) throws JsonProcessingException {
        String feedbackId = extractFeedbackId(event);
        if (feedbackId == null || feedbackId.isBlank()) {
            return response(400, "Parametro id é obrigatório");
        }

        Map<String, AttributeValue> item = repository.findById(feedbackId);
        if (item == null || item.isEmpty()) {
            context.getLogger().log("[" + requestId + "] Feedback nao encontrado: id=" + feedbackId);
            return response(404, "Feedback nao encontrado");
        }

        Map<String, Object> payload = toResponsePayload(item);
        return jsonResponse(200, payload);
    }

    private String extractFeedbackId(APIGatewayProxyRequestEvent event) {
        Map<String, String> pathParameters = event.getPathParameters();
        if (pathParameters != null && pathParameters.containsKey("id")) {
            return pathParameters.get("id");
        }

        String path = event.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }

        int index = path.lastIndexOf('/');
        if (index < 0 || index + 1 >= path.length()) {
            return null;
        }
        return path.substring(index + 1);
    }

    private Map<String, Object> toResponsePayload(Map<String, AttributeValue> item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", getString(item, "id"));
        payload.put("cursoId", getString(item, "cursoId"));
        payload.put("alunoId", getString(item, "alunoId"));
        payload.put("professorId", getString(item, "professorId"));
        payload.put("nota", getInteger(item, "nota"));
        payload.put("comentario", getString(item, "comentario"));
        payload.put("criticidade", getString(item, "criticidade"));
        payload.put("dataCriacao", getString(item, "dataCriacao"));
        return payload;
    }

    private String getString(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value == null) {
            return "";
        }
        return value.s() == null ? "" : value.s();
    }

    private Integer getInteger(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value == null || value.n() == null || value.n().isBlank()) {
            return null;
        }
        return Integer.parseInt(value.n());
    }

    private String resolveBody(APIGatewayProxyRequestEvent event) {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            return body;
        }

        if (Boolean.TRUE.equals(event.getIsBase64Encoded())) {
            return new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }

        return body;
    }

    private APIGatewayProxyResponseEvent response(int statusCode, String message) {
        return jsonResponse(statusCode, Map.of("message", message));
    }

    private APIGatewayProxyResponseEvent jsonResponse(int statusCode, Object payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            body = "{\"message\":\"Erro ao serializar resposta\"}";
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
            .withHeaders(RESPONSE_HEADERS)
                .withBody(body);
    }

    private APIGatewayProxyResponseEvent emptyResponse(int statusCode) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(RESPONSE_HEADERS)
                .withBody("");
    }
}
