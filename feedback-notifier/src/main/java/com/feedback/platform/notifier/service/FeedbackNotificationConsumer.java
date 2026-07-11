package com.feedback.platform.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.feedback.platform.notifier.dto.FeedbackEventDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.List;

@ApplicationScoped
public class FeedbackNotificationConsumer {

    private static final Logger LOG = Logger.getLogger(FeedbackNotificationConsumer.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final String queueName;

    @Inject
    public FeedbackNotificationConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            NotificationService notificationService,
            @ConfigProperty(name = "aws.sqs.queue-name", defaultValue = "feedback-critico-queue") String queueName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.queueName = queueName;
    }

    @Scheduled(every = "5s", delayed = "3s")
    void consumirFila() {
        String queueUrl = resolveQueueUrl();
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)
                .build();

        try {
            List<Message> messages = sqsClient.receiveMessage(request).messages();
            for (Message message : messages) {
                processarMensagemComAck(message);
            }
        } catch (Exception exception) {
            LOG.errorf(exception, "Falha ao consumir mensagens da fila SQS. queueUrl=%s", queueUrl);
        }
    }

    private void processarMensagemComAck(Message message) {
        String queueUrl = resolveQueueUrl();
        processarMensagem(message.body());
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }

    public void processarMensagem(String messageBody) {
        LOG.info("Mensagem recebida: " + messageBody);

        try {
            FeedbackEventDTO feedbackEventDTO = parseFeedbackEvent(messageBody);

            LOG.infof("Processando feedback crítico: %s para professor: %s",
                    feedbackEventDTO.feedbackId(),
                    feedbackEventDTO.professorId());

            notificationService.procesarNotificacao(feedbackEventDTO);

        } catch (JsonProcessingException exception) {
            LOG.errorf(exception,
                    "Mensagem irrecuperável: falha de formato JSON ao desserializar payload da fila SQS. payload=%s",
                    messageBody);
            // Boa prática: payload inválido deve ser roteado para a DLQ para inspeção manual
            // e não reprocessado indefinidamente.
        } catch (RuntimeException exception) {
            LOG.errorf(exception,
                    "Falha no processamento da mensagem da fila SQS. payload=%s",
                    messageBody);
        }
    }

    public void simularRecebimento(String mensagem) {
        processarMensagem(mensagem);
    }

    public String queueUrl() {
        return resolveQueueUrl();
    }

    public SqsClient sqsClient() {
        return sqsClient;
    }

    private String resolveQueueUrl() {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .queueUrl();
    }

    private FeedbackEventDTO parseFeedbackEvent(String messageBody) throws JsonProcessingException {
        if (messageBody.contains("\"detail-type\"") && messageBody.contains("\"detail\"")) {
            JsonNode root = objectMapper.readTree(messageBody);
            JsonNode detailNode = root.get("detail");
            if (detailNode == null || detailNode.isNull()) {
                throw new JsonProcessingException("Campo detail ausente no envelope do EventBridge") {
                };
            }

            if (detailNode.isTextual()) {
                return objectMapper.readValue(detailNode.asText(), FeedbackEventDTO.class);
            }

            return objectMapper.treeToValue(detailNode, FeedbackEventDTO.class);
        }
        return objectMapper.readValue(messageBody, FeedbackEventDTO.class);
    }
}
