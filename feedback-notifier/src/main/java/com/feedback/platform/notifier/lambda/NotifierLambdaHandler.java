package com.feedback.platform.notifier.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.feedback.platform.notifier.repository.RepositorioNotificacao;
import com.feedback.platform.notifier.repository.EnviadorNotificacao;
import com.feedback.platform.notifier.repository.dynamodb.RepositorioNotificacaoDynamoDB;
import com.feedback.platform.notifier.repository.ses.EnviadorNotificacaoSes;
import com.feedback.platform.notifier.service.ServicoNotificacao;
import com.feedback.platform.notifier.service.impl.ServicoNotificacaoImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ses.SesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotifierLambdaHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierLambdaHandler.class);

    private final ServicoNotificacao notificationService;

    public NotifierLambdaHandler() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.notificationService = buildService(objectMapper);
    }

    NotifierLambdaHandler(ServicoNotificacao notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        int records = event == null || event.getRecords() == null ? 0 : event.getRecords().size();
        context.getLogger().log("[" + requestId + "] Processando lote SQS de notificacoes. records=" + records);

        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            return new SQSBatchResponse(List.of());
        }

        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                String body = message.getBody();
                if (body == null || body.isBlank()) {
                    throw new IllegalArgumentException("Mensagem SQS vazia");
                }

                notificationService.simularRecebimento(body);
            } catch (Exception exception) {
                LOGGER.error("Falha ao processar mensagem SQS. messageId={}", message.getMessageId(), exception);
                failures.add(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
            }
        }

        return new SQSBatchResponse(failures);
    }

    private ServicoNotificacao buildService(ObjectMapper mapper) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String region = readEnv("AWS_REGION", readEnv("AWS_DEFAULT_REGION", "us-east-2"));
        String tableName = readEnv("AWS_DYNAMODB_NOTIFICATION_TABLE", readEnv("AWS_DYNAMODB_TABLE", "NotificacaoTable"));
        String fromEmail = readEnv("AWS_SES_FROM_EMAIL", "rafael.mendonca.brito@gmail.com");
        String toEmailOverride = readEnv("AWS_SES_TO_EMAIL_OVERRIDE", "rafael.mendonca.brito@gmail.com");

        LOGGER.info("AWS_REGION={}", region);
        LOGGER.info("FROM={}", fromEmail);
        LOGGER.info("TO={}", toEmailOverride);

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .build();
        SesClient sesClient = SesClient.builder()
                .region(Region.of(region))
                .build();

        RepositorioNotificacao repository = new RepositorioNotificacaoDynamoDB(dynamoDbClient, tableName);
        EnviadorNotificacao sender = new EnviadorNotificacaoSes(sesClient, fromEmail, toEmailOverride);

        return new ServicoNotificacaoImpl(repository, sender, mapper, validator);
    }

    private String readEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}