package com.feedback.platform.notifier.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.dto.FeedbackEventDTO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    @InjectMocks
    private NotificationServiceImpl service;

    private FeedbackEventDTO validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new FeedbackEventDTO(
                "feedback-123",
                "aluno-001",
                "professor-001",
                "ALTA"
        );
    }

    @Test
    void testProcessarNotificacao_Valida() {
        // Arrange
        Notificacao expected = new Notificacao(
                "notif-123",
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

        when(repository.salvar(any(Notificacao.class))).thenReturn(expected);
        doNothing().when(repository).enviarViaSES(any(Notificacao.class));

        // Act
        Notificacao result = service.procesarNotificacao(validEvent);

        // Assert
        assertNotNull(result);
        assertEquals(expected.feedbackId(), result.feedbackId());
        assertEquals("ALTA", result.urgencia());
        verify(repository, times(1)).salvar(any(Notificacao.class));
        verify(repository, times(1)).enviarViaSES(any(Notificacao.class));
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
        
        when(objectMapper.readValue(messageBody, FeedbackEventDTO.class))
                .thenReturn(new FeedbackEventDTO("f-1", "a-1", "p-1", "CRITICA"));
        when(repository.salvar(any(Notificacao.class)))
                .thenReturn(new Notificacao("id", "f-1", "p-1", "a-1", "CRITICA", "email", "assunto", "corpo",
                        Notificacao.StatusNotificacao.PENDENTE, java.time.Instant.now(), null));
        doNothing().when(repository).enviarViaSES(any(Notificacao.class));

        // Act & Assert
        assertDoesNotThrow(() -> service.simularRecebimento(messageBody));
        verify(objectMapper, times(1)).readValue(messageBody, FeedbackEventDTO.class);
    }
}
