package com.feedback.platform.notifier.repository.ses;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.repository.NotificationSender;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@ApplicationScoped
public class SesNotificationSender implements NotificationSender {

        private static final Logger LOGGER = LoggerFactory.getLogger(SesNotificationSender.class);
        private static final DateTimeFormatter EMAIL_DATE_FORMATTER = DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

        private final SesClient sesClient;
        private final String fromEmail;
        private final String toEmailOverride;

        @Inject
        public SesNotificationSender(
                        SesClient sesClient,
                        @ConfigProperty(name = "aws.ses.from-email", defaultValue = "no-reply@feedback-platform.local") String fromEmail,
                        @ConfigProperty(name = "aws.ses.to-email-override") Optional<String> toEmailOverride) {
                this(sesClient, fromEmail, toEmailOverride.orElse(""));
        }

        public SesNotificationSender(SesClient sesClient, String fromEmail) {
                this(sesClient, fromEmail, "");
        }

        public SesNotificationSender(SesClient sesClient, String fromEmail, String toEmailOverride) {
                this.sesClient = sesClient;
                this.fromEmail = fromEmail;
                this.toEmailOverride = toEmailOverride == null ? "" : toEmailOverride;
        }

        @Override
        public void enviar(Notificacao notificacao) {
                String destinationEmail = toEmailOverride.isBlank() ? notificacao.email() : toEmailOverride;
                String bodyWithFormattedDate = formatBodyWithDate(notificacao.corpo());

                SendEmailRequest request = SendEmailRequest.builder()
                                .source(fromEmail)
                                .destination(Destination.builder().toAddresses(destinationEmail).build())
                                .message(Message.builder()
                                                .subject(Content.builder().data(notificacao.assunto()).build())
                                                .body(Body.builder()
                                                                .text(Content.builder().data(bodyWithFormattedDate)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build();

                try {
                        sesClient.sendEmail(request);
                } catch (SesException e) {
                        LOGGER.error("SES Error: {}", e.awsErrorDetails().errorMessage(), e);
                        throw e;
                }

                if (toEmailOverride.isBlank()) {
                        LOGGER.info("Email SES enviado para {}", destinationEmail);
                } else {
                        LOGGER.info("Email SES enviado para override {} (destino original={})", destinationEmail,
                                        notificacao.email());
                }
        }

        private String formatBodyWithDate(String body) {
                String formattedDate = EMAIL_DATE_FORMATTER.format(Instant.now());

                if (body.contains("- Data:")) {
                        return body.replaceFirst("(?m)(- Data:\\s*).*$", "$1" + formattedDate);
                }

                return body + System.lineSeparator() + "- Data: " + formattedDate;
        }
}
