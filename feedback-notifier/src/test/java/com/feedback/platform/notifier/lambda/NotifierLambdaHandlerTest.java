package com.feedback.platform.notifier.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.feedback.platform.notifier.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifierLambdaHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private NotifierLambdaHandler handler;

    @BeforeEach
    void setUp() {
        when(context.getAwsRequestId()).thenReturn("req-1");
        when(context.getLogger()).thenReturn(logger);
        handler = new NotifierLambdaHandler(notificationService);
    }

    @Test
    void sqsBatchWithValidMessageReturnsNoFailures() {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("msg-1");
        message.setBody("{\"feedbackId\":\"fb-1\",\"alunoId\":\"aluno-1\",\"professorId\":\"prof-1\",\"urgencia\":\"ALTA\"}");
        event.setRecords(List.of(message));

        doNothing().when(notificationService).simularRecebimento(anyString());

        SQSBatchResponse response = handler.handleRequest(event, context);

        assertEquals(0, response.getBatchItemFailures().size());
        verify(notificationService, times(1)).simularRecebimento(anyString());
    }

    @Test
    void sqsBatchWithFailureReturnsBatchItemFailure() {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("msg-err");
        message.setBody("{\"feedbackId\":\"fb-2\",\"alunoId\":\"aluno-2\",\"professorId\":\"prof-2\",\"urgencia\":\"ALTA\"}");
        event.setRecords(List.of(message));

        doThrow(new RuntimeException("falha")).when(notificationService).simularRecebimento(anyString());

        SQSBatchResponse response = handler.handleRequest(event, context);

        assertEquals(1, response.getBatchItemFailures().size());
        assertEquals("msg-err", response.getBatchItemFailures().get(0).getItemIdentifier());
    }
}