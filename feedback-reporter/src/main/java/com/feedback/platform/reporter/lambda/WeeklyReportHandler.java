package com.feedback.platform.reporter.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import com.feedback.platform.reporter.repository.dynamodb.DynamoDBFeedbackRepository;
import com.feedback.platform.reporter.service.FeedbackReportService;
import com.feedback.platform.reporter.service.impl.FeedbackReportServiceImpl;
import com.feedback.platform.reporter.service.weekly.WeeklyReportSchedulerService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

public class WeeklyReportHandler implements RequestHandler<ScheduledEvent, ReportSemanalResponseDTO> {

    private final WeeklyReportSchedulerService weeklyReportSchedulerService;
    private final String weeklyReportCursoId;
    private final String weeklyReportProfessorId;

    public WeeklyReportHandler() {
        this.weeklyReportSchedulerService = new WeeklyReportSchedulerService(buildService());
        this.weeklyReportCursoId = readEnv("AWS_WEEKLY_REPORT_COURSE_ID", "");
        this.weeklyReportProfessorId = readEnv("AWS_WEEKLY_REPORT_PROFESSOR_ID", "");
    }

    WeeklyReportHandler(WeeklyReportSchedulerService weeklyReportSchedulerService,
                        String weeklyReportCursoId,
                        String weeklyReportProfessorId) {
        this.weeklyReportSchedulerService = weeklyReportSchedulerService;
        this.weeklyReportCursoId = weeklyReportCursoId == null ? "" : weeklyReportCursoId;
        this.weeklyReportProfessorId = weeklyReportProfessorId == null ? "" : weeklyReportProfessorId;
    }

    @Override
    public ReportSemanalResponseDTO handleRequest(ScheduledEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        String cursoId = weeklyReportCursoId == null ? "" : weeklyReportCursoId.trim();

        if (cursoId.isBlank()) {
            throw new IllegalArgumentException("AWS_WEEKLY_REPORT_COURSE_ID e obrigatorio para a execucao agendada");
        }

        String professorId = normalizeProfessorId(weeklyReportProfessorId);
        context.getLogger().log("[" + requestId + "] Iniciando relatorio semanal agendado. cursoId=" + cursoId
                + " professorId=" + (professorId == null ? "<todos>" : professorId));

        ReportSemanalResponseDTO response = weeklyReportSchedulerService.generateWeeklyCourseReport(cursoId, professorId);

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