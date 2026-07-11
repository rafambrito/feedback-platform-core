package com.feedback.platform.notifier.repository.ses;

import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.repository.NotificationSender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@ApplicationScoped
public class SesNotificationSender implements NotificationSender {

    private static final Logger LOG = Logger.getLogger(SesNotificationSender.class);

    private final SesClient sesClient;
    private final String fromEmail;

    @Inject
    public SesNotificationSender(
            SesClient sesClient,
            @ConfigProperty(name = "aws.ses.from-email", defaultValue = "no-reply@feedback-platform.local") String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void enviar(Notificacao notificacao) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(notificacao.email()).build())
                .message(Message.builder()
                        .subject(Content.builder().data(notificacao.assunto()).build())
                        .body(Body.builder()
                                .text(Content.builder().data(notificacao.corpo()).build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
        LOG.infof("Email SES enviado para %s", notificacao.email());
    }
}
