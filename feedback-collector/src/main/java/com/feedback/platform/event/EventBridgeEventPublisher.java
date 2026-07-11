package com.feedback.platform.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.Map;

@ApplicationScoped
public class EventBridgeEventPublisher implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(EventBridgeEventPublisher.class);
    private static final String SOURCE = "com.feedback.platform";
    private static final String DETAIL_TYPE = "FeedbackCriticoEvent";
    private static final String EVENT_BUS_NAME = "default";

    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;

    @Inject
    public EventBridgeEventPublisher(EventBridgeClient eventBridgeClient, ObjectMapper objectMapper) {
        this.eventBridgeClient = eventBridgeClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishCriticalFeedback(String feedbackId, String alunoId, String professorId) {
        try {
            String detail = objectMapper.writeValueAsString(Map.of(
                    "feedbackId", feedbackId,
                    "alunoId", alunoId,
                    "professorId", professorId,
                    "urgencia", "ALTA"
            ));

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(EVENT_BUS_NAME)
                    .source(SOURCE)
                    .detailType(DETAIL_TYPE)
                    .detail(detail)
                    .build();

            PutEventsResponse response = eventBridgeClient.putEvents(
                    PutEventsRequest.builder().entries(entry).build());

            if (response.failedEntryCount() > 0) {
                LOG.warnv("EventBridge rejeitou {0} entrada(s) para feedbackId={1}",
                        response.failedEntryCount(), feedbackId);
            } else {
                LOG.infov("Evento publicado no EventBridge: feedbackId={0}, detailType={1}",
                        feedbackId, DETAIL_TYPE);
            }

        } catch (Exception e) {
            LOG.errorv(e, "Falha ao publicar evento no EventBridge para feedbackId={0}. O feedback foi salvo normalmente.", feedbackId);
        }
    }
}