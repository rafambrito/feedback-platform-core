package com.feedback.platform.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedback.platform.notifier.dto.FeedbackEventDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class FeedbackNotificationConsumer {

    private static final Logger LOG = Logger.getLogger(FeedbackNotificationConsumer.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    @Inject
    public FeedbackNotificationConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "aws.sqs.queue-url") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    public void processarMensagem(String messageBody) {
        LOG.info("Mensagem recebida: " + messageBody);

        try {
            FeedbackEventDTO feedbackEventDTO = objectMapper.readValue(messageBody, FeedbackEventDTO.class);

            LOG.infof("Processando feedback crítico: %s para professor: %s",
                    feedbackEventDTO.feedbackId(),
                    feedbackEventDTO.professorId());

            LOG.infof("Enviando notificação para o professor... professorId=%s",
                    feedbackEventDTO.professorId());

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
        return queueUrl;
    }

    public SqsClient sqsClient() {
        return sqsClient;
    }
}
