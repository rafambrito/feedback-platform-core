package com.feedback.platform.notifier.resource;

import com.feedback.platform.dto.UrgencyNotification;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.service.NotificationService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class NotificationResourceTest {

    @InjectMock
    private NotificationService notificationService;

    @Test
    void testEnviarNotificacaoUrgente_Sucesso() {
        // Arrange
                UrgencyNotification request = new UrgencyNotification(
                "feedback-123",
                "aluno-001",
                "professor-001",
                "ALTA"
        );

        Notificacao notificacao = new Notificacao(
                "notif-123",
                "feedback-123",
                "professor-001",
                "aluno-001",
                "ALTA",
                "professor-001@universidade.edu.br",
                "⚠️ ALTA: Feedback Importante Recebido",
                "Corpo do email",
                Notificacao.StatusNotificacao.PENDENTE,
                Instant.now(),
                null
        );

        when(notificationService.processarNotificacao(any(UrgencyNotification.class)))
                .thenReturn(notificacao);

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/notifications/urgent")
                .then()
                .statusCode(201)
                .body("id", equalTo("notif-123"))
                .body("feedbackId", equalTo("feedback-123"))
                .body("status", equalTo("PENDENTE"));

                verify(notificationService, times(1)).processarNotificacao(any(UrgencyNotification.class));
    }

    @Test
    void testEnviarNotificacaoUrgente_RequestInvalido() {
        // Arrange - payload inválido (sem campos obrigatórios)
        String invalidPayload = "{}";

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .post("/notifications/urgent")
                .then()
                .statusCode(anyOf(is(400), is(422))); // 400 Bad Request ou 422 Unprocessable Entity
    }

    @Test
    void testBuscarNotificacao_Encontrada() {
        // Arrange
        Notificacao notificacao = new Notificacao(
                "notif-123",
                "feedback-123",
                "professor-001",
                "aluno-001",
                "ALTA",
                "professor-001@universidade.edu.br",
                "Assunto",
                "Corpo",
                Notificacao.StatusNotificacao.ENVIADA,
                Instant.now(),
                Instant.now()
        );

        when(notificationService.buscarPorId("notif-123"))
                .thenReturn(notificacao);

        // Act & Assert
        given()
                .when()
                .get("/notifications/notif-123")
                .then()
                .statusCode(200)
                .body("id", equalTo("notif-123"))
                .body("status", equalTo("ENVIADA"));
    }

    @Test
    void testBuscarNotificacao_NaoEncontrada() {
        // Arrange
        when(notificationService.buscarPorId("inexistente"))
                .thenReturn(null);

        // Act & Assert
        given()
                .when()
                .get("/notifications/inexistente")
                .then()
                .statusCode(404);
    }

    @Test
    void testSimularRecebimento_Sucesso() {
        // Arrange
        String messageBody = "{\"feedbackId\":\"f-1\",\"alunoId\":\"a-1\",\"professorId\":\"p-1\",\"urgencia\":\"CRITICA\"}";

        doNothing().when(notificationService).simularRecebimento(messageBody);

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body(messageBody)
                .when()
                .post("/notifications/test/simulate")
                .then()
                .statusCode(200)
                .body("message", equalTo("Simulação processada com sucesso"));
    }
}
