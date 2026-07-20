package com.feedback.platform.reporter.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import com.feedback.platform.reporter.repository.dynamodb.DynamoDBFeedbackRepository;
import com.feedback.platform.reporter.service.FeedbackReportService;
import com.feedback.platform.reporter.service.impl.FeedbackReportServiceImpl;
import com.feedback.platform.reporter.service.semanal.RelatorioSemanalSchedulerService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

public class SemanalReportHandler implements RequestHandler<ScheduledEvent, ReportSemanalResponseDTO> {

    private final RelatorioSemanalSchedulerService relatorioSemanalSchedulerService;
    private final String relatorioSemanalCursoId;
    private final String relatorioSemanalProfessorId;

    public SemanalReportHandler() {
        this.relatorioSemanalSchedulerService = new RelatorioSemanalSchedulerService(buildService());
        this.relatorioSemanalCursoId = readEnv("AWS_RELATORIO_SEMANAL_CURSO_ID", readEnv("AWS_WEEKLY_REPORT_COURSE_ID", ""));
        this.relatorioSemanalProfessorId = readEnv("AWS_RELATORIO_SEMANAL_PROFESSOR_ID", readEnv("AWS_WEEKLY_REPORT_PROFESSOR_ID", ""));
    }

    SemanalReportHandler(RelatorioSemanalSchedulerService relatorioSemanalSchedulerService,
                         String relatorioSemanalCursoId,
                         String relatorioSemanalProfessorId) {
        this.relatorioSemanalSchedulerService = relatorioSemanalSchedulerService;
        this.relatorioSemanalCursoId = relatorioSemanalCursoId == null ? "" : relatorioSemanalCursoId;
        this.relatorioSemanalProfessorId = relatorioSemanalProfessorId == null ? "" : relatorioSemanalProfessorId;
    }

    @Override
    public ReportSemanalResponseDTO handleRequest(ScheduledEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        String cursoId = relatorioSemanalCursoId == null ? "" : relatorioSemanalCursoId.trim();

        if (cursoId.isBlank()) {
            throw new IllegalArgumentException("AWS_RELATORIO_SEMANAL_CURSO_ID e obrigatorio para a execucao agendada");
        }

        String professorId = normalizeProfessorId(relatorioSemanalProfessorId);
        context.getLogger().log("[" + requestId + "] Iniciando relatorio semanal agendado. cursoId=" + cursoId
                + " professorId=" + (professorId == null ? "<todos>" : professorId));

        ReportSemanalResponseDTO response = relatorioSemanalSchedulerService.gerarRelatorioSemanalCurso(cursoId, professorId);

        context.getLogger().log("[" + requestId + "] Relatorio semanal gerado com sucesso. totalFeedbacks="
                + response.totalFeedbacks() + " geradoEm=" + response.geradoEm());

        return response;
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

    private String normalizeProfessorId(String professorId) {
        if (professorId == null) {
            return null;
        }

        String normalized = professorId.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String readEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}