package com.feedback.platform.notifier.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedback.platform.dto.UrgencyNotification;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.repository.NotificationSender;
import com.feedback.platform.notifier.repository.NotificationRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    @InjectMocks
    private NotificationServiceImpl service;

    private UrgencyNotification validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new UrgencyNotification(
                "feedback-123",
                "aluno-001",
                "professor-001",
                "ALTA"
        );

        lenient().when(validator.validate(any(UrgencyNotification.class))).thenReturn(Collections.emptySet());
    }

    @Test
    void testProcessarNotificacao_Valida() {
        // Arrange
        Notificacao expected = new Notificacao(
            "550e8400-e29b-41d4-a716-446655440000",
                "feedback-123",
                "professor-001",
                "aluno-001",
                "ALTA",
                "professor-001@universidade.edu.br",
                "⚠️ ALTA: Feedback Importante Recebido",
                "Corpo do email",
                Notificacao.StatusNotificacao.PENDENTE,
                java.time.Instant.now(),
                null
        );

            when(repository.salvarSeAusente(any(Notificacao.class))).thenReturn(expected);
            doNothing().when(notificationSender).enviar(any(Notificacao.class));
            when(repository.buscarPorId(anyString())).thenReturn(new Notificacao(
                expected.id(),
                expected.feedbackId(),
                expected.professorId(),
                expected.alunoId(),
                expected.urgencia(),
                expected.email(),
                expected.assunto(),
                expected.corpo(),
                Notificacao.StatusNotificacao.ENVIADA,
                expected.dataCriacao(),
                java.time.Instant.now()
            ));

        // Act
            Notificacao result = service.processarNotificacao(validEvent);

        // Assert
        assertNotNull(result);
        assertEquals(expected.feedbackId(), result.feedbackId());
            assertEquals(Notificacao.StatusNotificacao.ENVIADA, result.status());
            verify(repository, times(1)).salvarSeAusente(any(Notificacao.class));
            verify(notificationSender, times(1)).enviar(any(Notificacao.class));
            verify(repository, times(1)).atualizarStatus(anyString(), eq(Notificacao.StatusNotificacao.ENVIADA));
            }

            @Test
            void testProcessarNotificacao_IdempotenteNaoReenvia() {
            Notificacao existente = new Notificacao(
                "idempotente-id",
                "feedback-123",
                "professor-001",
                "aluno-001",
                "ALTA",
                "professor-001@universidade.edu.br",
                "assunto",
                "corpo",
                Notificacao.StatusNotificacao.ENVIADA,
                java.time.Instant.now(),
                java.time.Instant.now()
            );

            when(repository.salvarSeAusente(any(Notificacao.class))).thenReturn(existente);

            Notificacao result = service.processarNotificacao(validEvent);

            assertNotNull(result);
            assertEquals(Notificacao.StatusNotificacao.ENVIADA, result.status());
            verify(notificationSender, never()).enviar(any(Notificacao.class));
            verify(repository, never()).atualizarStatus(any(), eq(Notificacao.StatusNotificacao.FALHA));
    }

    @Test
    void testBuscarPorId_Encontrada() {
        // Arrange
        Notificacao expected = new Notificacao(
                "notif-123",
                "feedback-123",
                "professor-001",
                "aluno-001",
                "ALTA",
                "professor-001@universidade.edu.br",
                "Assunto",
                "Corpo",
                Notificacao.StatusNotificacao.ENVIADA,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(repository.buscarPorId("notif-123")).thenReturn(expected);

        // Act
        Notificacao result = service.buscarPorId("notif-123");

        // Assert
        assertNotNull(result);
        assertEquals("notif-123", result.id());
        assertEquals(Notificacao.StatusNotificacao.ENVIADA, result.status());
    }

    @Test
    void testBuscarPorId_NaoEncontrada() {
        // Arrange
        when(repository.buscarPorId("inexistente")).thenReturn(null);

        // Act
        Notificacao result = service.buscarPorId("inexistente");

        // Assert
        assertNull(result);
    }

    @Test
    void testSimularRecebimento_Valido() throws Exception {
        // Arrange
        String messageBody = "{\"feedbackId\":\"f-1\",\"alunoId\":\"a-1\",\"professorId\":\"p-1\",\"urgencia\":\"CRITICA\"}";

        when(objectMapper.readValue(messageBody, UrgencyNotification.class))
            .thenReturn(new UrgencyNotification("f-1", "a-1", "p-1", "CRITICA"));
        when(repository.salvarSeAusente(any(Notificacao.class)))
                .thenReturn(new Notificacao("id", "f-1", "p-1", "a-1", "CRITICA", "email", "assunto", "corpo",
                        Notificacao.StatusNotificacao.PENDENTE, java.time.Instant.now(), null));
        doNothing().when(notificationSender).enviar(any(Notificacao.class));
        when(repository.buscarPorId(anyString())).thenReturn(new Notificacao(
            "id", "f-1", "p-1", "a-1", "CRITICA", "email", "assunto", "corpo",
            Notificacao.StatusNotificacao.ENVIADA, java.time.Instant.now(), java.time.Instant.now()));

        // Act & Assert
        assertDoesNotThrow(() -> service.simularRecebimento(messageBody));
        verify(objectMapper, times(1)).readValue(messageBody, UrgencyNotification.class);
    }
}
