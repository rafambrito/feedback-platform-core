package com.feedback.platform.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBridgeEventPublisherTest {

    @Mock
    EventBridgeClient eventBridgeClient;

    @Captor
    ArgumentCaptor<PutEventsRequest> requestCaptor;

    EventBridgeEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new EventBridgeEventPublisher(eventBridgeClient, new ObjectMapper());
    }

    @Test
    void sucesso_envia_evento_com_campos_corretos() {
        PutEventsResponse response = PutEventsResponse.builder().failedEntryCount(0).build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);

        publisher.publishCriticalFeedback("fb-1", "aluno-1", "prof-1");

        verify(eventBridgeClient).putEvents(requestCaptor.capture());
        PutEventsRequest sent = requestCaptor.getValue();
        assertEquals(1, sent.entries().size());

        var entry = sent.entries().get(0);
        assertEquals("feedback-events", entry.eventBusName());
        assertEquals("com.feedback.platform", entry.source());
        assertEquals("FeedbackCriticoEvent", entry.detailType());
        assertTrue(entry.detail().contains("\"feedbackId\":\"fb-1\""));
        assertTrue(entry.detail().contains("\"alunoId\":\"aluno-1\""));
        assertTrue(entry.detail().contains("\"professorId\":\"prof-1\""));
    }

    @Test
    void falha_parcial_registrada_mas_nao_propaga_excecao() {
        PutEventsResponse response = PutEventsResponse.builder().failedEntryCount(1).build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);

        assertDoesNotThrow(() -> publisher.publishCriticalFeedback("fb-2", "a", "p"));
        verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void excecao_do_cliente_nao_propaga_para_chamador() {
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
                .thenThrow(new RuntimeException("AWS indisponível"));

        assertDoesNotThrow(() -> publisher.publishCriticalFeedback("fb-3", "a", "p"));
    }
}
