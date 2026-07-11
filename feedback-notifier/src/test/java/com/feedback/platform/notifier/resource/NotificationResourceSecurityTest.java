package com.feedback.platform.notifier.resource;

import com.feedback.platform.dto.UrgencyNotification;
import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.service.NotificationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(SecurityEnabledTestProfile.class)
class NotificationResourceSecurityTest {

    @InjectMock
    NotificationService notificationService;

    @Test
    void postUrgente_semToken_retorna401() {
        given()
                .contentType(ContentType.JSON)
                .body(new UrgencyNotification("feedback-1", "aluno-1", "prof-1", "ALTA"))
                .when()
                .post("/notifications/urgent")
                .then()
                .statusCode(401)
                .body("error_code", equalTo("TOKEN_INVALID"));
    }

    @Test
    void postUrgente_tokenInvalido_retorna401() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer INVALID_TOKEN")
                .body(new UrgencyNotification("feedback-1", "aluno-1", "prof-1", "ALTA"))
                .when()
                .post("/notifications/urgent")
                .then()
                .statusCode(401)
                .body("error_code", equalTo("TOKEN_INVALID"));
    }

    @Test
    void postUrgente_tokenValido_retorna201() {
        Notificacao notificacao = new Notificacao(
                "notif-sec-1",
                "feedback-1",
                "prof-1",
                "aluno-1",
                "ALTA",
                "professor-1@feedback.local",
                "Assunto",
                "Corpo",
                Notificacao.StatusNotificacao.PENDENTE,
                Instant.now(),
                null
        );

        when(notificationService.processarNotificacao(any(UrgencyNotification.class))).thenReturn(notificacao);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SecurityEnabledTestProfile.TOKENS.generateValidToken())
                .body(new UrgencyNotification("feedback-1", "aluno-1", "prof-1", "ALTA"))
                .when()
                .post("/notifications/urgent")
                .then()
                .statusCode(201)
                .body("id", equalTo("notif-sec-1"));
    }
}
