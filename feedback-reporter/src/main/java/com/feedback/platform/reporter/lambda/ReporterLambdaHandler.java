package com.feedback.platform.reporter.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import com.feedback.platform.reporter.repository.dynamodb.DynamoDBFeedbackRepository;
import com.feedback.platform.reporter.service.FeedbackReportService;
import com.feedback.platform.reporter.service.impl.FeedbackReportServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;
import java.util.Optional;

public class ReporterLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "https://rafambrito.github.io",
            "Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token"
    );

    private final ObjectMapper objectMapper;
    private final FeedbackReportService feedbackReportService;

    public ReporterLambdaHandler() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.feedbackReportService = buildService();
    }

    ReporterLambdaHandler(FeedbackReportService feedbackReportService) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.feedbackReportService = feedbackReportService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        String method = event.getHttpMethod() == null ? "" : event.getHttpMethod().toUpperCase();
        String path = event.getPath() == null ? "" : event.getPath();

        context.getLogger().log("[" + requestId + "] Processando relatorio. method=" + method + " path=" + path);

        try {
            if ("OPTIONS".equals(method)) {
                return emptyResponse(204);
            }

            if (!"GET".equals(method)) {
                return response(405, Map.of("message", "Metodo nao suportado"));
            }

            if (path.contains("/reports/professor/")) {
                String professorId = extractLastPathSegment(path);
                if (professorId == null || professorId.isBlank()) {
                    return response(400, Map.of("message", "professorId e obrigatorio"));
                }
                return response(200, feedbackReportService.getProfessorReport(professorId));
            }

            if (path.contains("/reports/curso/")) {
                String cursoId = extractLastPathSegment(path);
                if (cursoId == null || cursoId.isBlank()) {
                    return response(400, Map.of("message", "cursoId e obrigatorio"));
                }
                return response(200, feedbackReportService.getCursoReport(cursoId));
            }

            if (path.endsWith("/reports/semanal")) {
                Map<String, String> query = event.getQueryStringParameters();
                String cursoId = query != null ? query.get("cursoId") : null;
                String professorId = query != null ? query.get("professorId") : null;

                if (cursoId == null || cursoId.isBlank()) {
                    return response(400, Map.of("message", "cursoId e obrigatorio"));
                }

                return response(200, feedbackReportService.getRelatorioSemanalCurso(cursoId, professorId));
            }

            return response(404, Map.of("message", "Endpoint nao encontrado"));
        } catch (Exception e) {
            context.getLogger().log("[" + requestId + "] Erro interno: " + e.getMessage());
            return response(500, Map.of("error", "Erro ao processar relatorio"));
        }
    }

    private FeedbackReportService buildService() {
        String region = readEnv("AWS_REGION", readEnv("AWS_DEFAULT_REGION", "us-east-2"));
        String tableName = readEnv("AWS_DYNAMODB_TABLE", "FeedbackTable");
        String cursoGsi = readEnv("AWS_DYNAMODB_GSI_CURSO_NAME", "");
        String professorGsi = readEnv("AWS_DYNAMODB_GSI_PROFESSOR_NAME", "");

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .build();

        FeedbackRepository repository = new DynamoDBFeedbackRepository(
                dynamoDbClient,
                tableName,
                Optional.ofNullable(cursoGsi),
                Optional.ofNullable(professorGsi)
        );

        return new FeedbackReportServiceImpl(repository);
    }

    private String readEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private String extractLastPathSegment(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        int index = path.lastIndexOf('/');
        if (index < 0 || index + 1 >= path.length()) {
            return null;
        }

        return path.substring(index + 1);
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
}
