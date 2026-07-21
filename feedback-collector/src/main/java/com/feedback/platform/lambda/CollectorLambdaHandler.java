package com.feedback.platform.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.feedback.platform.domain.Criticidade;
import com.feedback.platform.dto.UrgencyNotification;
import com.feedback.platform.lambda.dto.AvaliacaoRequestDTO;
import com.feedback.platform.lambda.dto.FeedbackRequestDTO;
import com.feedback.platform.lambda.repository.LambdaFeedbackRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectorLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String AVALIACAO_PATH = "/avaliacao";
    private static final String DEFAULT_CURSO_ID = "CURSO-GERAL";
    private static final String DEFAULT_ALUNO_ID = "ALUNO-ANONIMO";
    private static final String DEFAULT_PROFESSOR_ID = "PROF-GERAL";

    private static final Map<String, String> RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "https://rafambrito.github.io",
            "Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token"
    );

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final LambdaFeedbackRepository repository;
    private final SqsClient sqsClient;
    private final String notificationQueueUrl;

    public CollectorLambdaHandler() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.repository = new LambdaFeedbackRepository();
        this.sqsClient = buildSqsClient();
        this.notificationQueueUrl = readEnv("AWS_NOTIFICATION_QUEUE_URL", "");
    }

    CollectorLambdaHandler(LambdaFeedbackRepository repository) {
        this(repository, null, "");
    }

    CollectorLambdaHandler(LambdaFeedbackRepository repository, SqsClient sqsClient, String notificationQueueUrl) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.repository = repository;
        this.sqsClient = sqsClient;
        this.notificationQueueUrl = notificationQueueUrl == null ? "" : notificationQueueUrl;
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

            FeedbackRequestDTO request = parseRequest(event, body);
            Set<ConstraintViolation<FeedbackRequestDTO>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining("; "));

                context.getLogger().log("[" + requestId + "] Falha de validação: " + details);
                return response(400, details);
            }

            LambdaFeedbackRepository.SavedFeedback feedback = repository.save(request, requestId);
            publishToNotificationQueueIfUrgent(feedback, context, requestId);
            context.getLogger().log("[" + requestId + "] Feedback persistido com sucesso");
            return response(201, "Feedback recebido com sucesso");

        } catch (IllegalArgumentException e) {
            context.getLogger().log("[" + requestId + "] Falha de validação: " + e.getMessage());
            return response(400, e.getMessage());
        } catch (JsonProcessingException e) {
            context.getLogger().log("[" + requestId + "] JSON inválido: " + e.getMessage());
            return response(400, "Payload JSON inválido");
        } catch (Exception e) {
            context.getLogger().log("[" + requestId + "] Erro interno: " + e.getMessage());
            return response(500, "Erro interno ao processar feedback");
        }
    }

    private FeedbackRequestDTO parseRequest(APIGatewayProxyRequestEvent event, String body) throws JsonProcessingException {
        String path = event.getPath() == null ? "" : event.getPath();
        if (path.endsWith(AVALIACAO_PATH)) {
            AvaliacaoRequestDTO avaliacaoRequest = objectMapper.readValue(body, AvaliacaoRequestDTO.class);
            Set<ConstraintViolation<AvaliacaoRequestDTO>> violations = validator.validate(avaliacaoRequest);
            if (!violations.isEmpty()) {
                return throwValidationError(violations);
            }

            return new FeedbackRequestDTO(
                    DEFAULT_CURSO_ID,
                    DEFAULT_ALUNO_ID,
                    DEFAULT_PROFESSOR_ID,
                    avaliacaoRequest.nota(),
                    avaliacaoRequest.descricao()
            );
        }

        return objectMapper.readValue(body, FeedbackRequestDTO.class);
    }

    private <T> FeedbackRequestDTO throwValidationError(Set<ConstraintViolation<T>> violations) {
        String details = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
        throw new IllegalArgumentException(details);
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

    private void publishToNotificationQueueIfUrgent(LambdaFeedbackRepository.SavedFeedback feedback,
                                                    Context context,
                                                    String requestId) {
        if (feedback.criticidade() != Criticidade.ALTA) {
            return;
        }

        if (notificationQueueUrl.isBlank()) {
            context.getLogger().log("[" + requestId + "] AWS_NOTIFICATION_QUEUE_URL ausente. Notificacao assincrona nao publicada.");
            return;
        }

        if (sqsClient == null) {
            context.getLogger().log("[" + requestId + "] Cliente SQS indisponivel. Notificacao assincrona nao publicada.");
            return;
        }

        try {
            UrgencyNotification payload = new UrgencyNotification(
                    feedback.id(),
                    feedback.alunoId(),
                    feedback.professorId(),
                    feedback.criticidade().name()
            );

            String body = objectMapper.writeValueAsString(payload);
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(notificationQueueUrl)
                    .messageBody(body)
                    .build();

            sqsClient.sendMessage(sendMessageRequest);
            context.getLogger().log("[" + requestId + "] Notificacao assincrona enviada para SQS. feedbackId=" + feedback.id());
        } catch (Exception exception) {
            context.getLogger().log("[" + requestId + "] Falha ao publicar notificacao no SQS: " + exception.getMessage());
        }
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

    private SqsClient buildSqsClient() {
        String region = readEnv("AWS_REGION", "us-east-1");
        String endpointOverride = readEnv("AWS_SQS_ENDPOINT_OVERRIDE", "");

        SqsClientBuilder builder = SqsClient.builder().region(Region.of(region));
        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
            return builder.build();
        }

        builder.credentialsProvider(DefaultCredentialsProvider.create());
        return builder.build();
    }

    private String readEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
