package com.feedback.platform.notifier.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.feedback.platform.dto.UrgencyNotification;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.repository.NotificationRepository;
import com.feedback.platform.notifier.repository.NotificationSender;
import com.feedback.platform.notifier.repository.dynamodb.DynamoDBNotificationRepository;
import com.feedback.platform.notifier.repository.ses.SesNotificationSender;
import com.feedback.platform.notifier.service.NotificationService;
import com.feedback.platform.notifier.service.impl.NotificationServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ses.SesClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotifierLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "https://rafambrito.github.io",
            "Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token"
    );

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public NotifierLambdaHandler() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.notificationService = buildService(this.objectMapper);
    }

    NotifierLambdaHandler(NotificationService notificationService) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.notificationService = notificationService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        String method = event.getHttpMethod() == null ? "" : event.getHttpMethod().toUpperCase();
        String path = event.getPath() == null ? "" : event.getPath();

        context.getLogger().log("[" + requestId + "] Processando notificacao. method=" + method + " path=" + path);

        try {
            if ("OPTIONS".equals(method)) {
                return emptyResponse(204);
            }

            if ("POST".equals(method) && path.endsWith("/notifications/urgent")) {
                return handleUrgentNotification(event);
            }

            if ("POST".equals(method) && path.endsWith("/notifications/test/simulate")) {
                return handleSimulate(event);
            }

            if ("GET".equals(method) && path.contains("/notifications/")) {
                return handleGetById(event);
            }

            return response(405, Map.of("message", "Metodo nao suportado"));
        } catch (IllegalArgumentException e) {
            context.getLogger().log("[" + requestId + "] Falha de validacao: " + e.getMessage());
            return response(400, Map.of("error", e.getMessage()));
        } catch (JsonProcessingException e) {
            context.getLogger().log("[" + requestId + "] JSON invalido: " + e.getMessage());
            return response(400, Map.of("error", "Payload JSON invalido"));
        } catch (Exception e) {
            context.getLogger().log("[" + requestId + "] Erro interno: " + e.getMessage());
            return response(500, Map.of("error", "Erro ao processar notificacao"));
        }
    }

    private APIGatewayProxyResponseEvent handleUrgentNotification(APIGatewayProxyRequestEvent event) throws JsonProcessingException {
        String body = resolveBody(event);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Payload invalido");
        }

        UrgencyNotification urgencyNotification = objectMapper.readValue(body, UrgencyNotification.class);
        Notificacao notificacao = notificationService.processarNotificacao(urgencyNotification);

        return response(201, toResponsePayload(notificacao));
    }

    private APIGatewayProxyResponseEvent handleGetById(APIGatewayProxyRequestEvent event) {
        String notificationId = extractNotificationId(event);
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("Parametro id e obrigatorio");
        }

        Notificacao notificacao = notificationService.buscarPorId(notificationId);
        if (notificacao == null) {
            return response(404, Map.of("message", "Notificacao nao encontrada"));
        }

        return response(200, toResponsePayload(notificacao));
    }

    private Map<String, Object> toResponsePayload(Notificacao notificacao) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", notificacao.id());
        payload.put("feedbackId", notificacao.feedbackId());
        payload.put("professorId", notificacao.professorId());
        payload.put("status", notificacao.status() != null ? notificacao.status().name() : null);
        payload.put("dataCriacao", notificacao.dataCriacao());
        payload.put("dataEnvio", notificacao.dataEnvio());
        return payload;
    }

    private APIGatewayProxyResponseEvent handleSimulate(APIGatewayProxyRequestEvent event) {
        String body = resolveBody(event);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Payload invalido");
        }

        notificationService.simularRecebimento(body);
        return response(200, Map.of("message", "Simulacao processada com sucesso"));
    }

    private String extractNotificationId(APIGatewayProxyRequestEvent event) {
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

    private APIGatewayProxyResponseEvent response(int statusCode, Object payload) {
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

    private NotificationService buildService(ObjectMapper mapper) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String region = readEnv("AWS_REGION", readEnv("AWS_DEFAULT_REGION", "us-east-1"));
        String tableName = readEnv("AWS_DYNAMODB_NOTIFICATION_TABLE", readEnv("AWS_DYNAMODB_TABLE", "NotificacaoTable"));
        String fromEmail = readEnv("AWS_SES_FROM_EMAIL", "no-reply@feedback-platform.local");
        String toEmailOverride = readEnv("AWS_SES_TO_EMAIL_OVERRIDE", "");

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .build();
        SesClient sesClient = SesClient.builder()
                .region(Region.of(region))
                .build();

        NotificationRepository repository = new DynamoDBNotificationRepository(dynamoDbClient, tableName);
        NotificationSender sender = new SesNotificationSender(sesClient, fromEmail, toEmailOverride);

        return new NotificationServiceImpl(repository, sender, mapper, validator);
    }

    private String readEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}