package com.feedback.platform.notifier.resource;

import com.feedback.platform.notifier.integration.DynamoDbLocalResource;
import com.feedback.platform.notifier.repository.EnviadorNotificacao;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(DynamoDbLocalResource.class)
@TestProfile(NoAuthTestProfile.class)
class NotificationIntegrationTest {

    private static final String TABLE_NAME = "NotificacaoTable";

    @Inject
    DynamoDbClient dynamoDbClient;

    @InjectMock
    EnviadorNotificacao enviadorNotificacao;

    @BeforeEach
    void setup() {
        clearTable();
        doNothing().when(enviadorNotificacao).enviar(any());
    }

    @Test
    void simulateEvent_persistsAndUpdatesStatusToEnviada() {
        String payload = """
                {
                  "feedbackId": "fb-it-1",
                  "alunoId": "aluno-it-1",
                  "professorId": "prof-it-1",
                  "urgencia": "ALTA"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/notifications/test/simulate")
                .then()
                .statusCode(200)
                .body("message", equalTo("Simulação processada com sucesso"));

        String id = notificationId("fb-it-1", "prof-it-1", "ALTA");
        given()
                .when()
                .get("/notifications/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("feedbackId", equalTo("fb-it-1"))
                .body("status", equalTo("ENVIADA"));
    }

    @Test
    void simulateEvent_withDetailEnvelope_deserializesAndPersists() {
        String payload = """
                {
                  "detail": {
                    "feedbackId": "fb-it-2",
                    "alunoId": "aluno-it-2",
                    "professorId": "prof-it-2",
                    "urgencia": "CRITICA"
                  }
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/notifications/test/simulate")
                .then()
                .statusCode(200);

        String id = notificationId("fb-it-2", "prof-it-2", "CRITICA");
        given()
                .when()
                .get("/notifications/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("status", equalTo("ENVIADA"));
    }

    @Test
    void simulateEvent_twice_isIdempotentAndDoesNotResend() {
        String payload = """
                {
                  "feedbackId": "fb-it-3",
                  "alunoId": "aluno-it-3",
                  "professorId": "prof-it-3",
                  "urgencia": "ALTA"
                }
                """;

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/notifications/test/simulate")
                .then().statusCode(200);

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/notifications/test/simulate")
                .then().statusCode(200);

        verify(enviadorNotificacao, times(1)).enviar(any());

        long itemCount = dynamoDbClient.scan(ScanRequest.builder().tableName(TABLE_NAME).build())
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(1, itemCount);
    }

    @Test
    void simulateEvent_whenSenderFails_updatesStatusToFalha() {
        doThrow(new RuntimeException("SES indisponível")).when(enviadorNotificacao).enviar(any());

        String payload = """
                {
                  "feedbackId": "fb-it-4",
                  "alunoId": "aluno-it-4",
                  "professorId": "prof-it-4",
                  "urgencia": "ALTA"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/notifications/test/simulate")
                .then()
                .statusCode(200);

        String id = notificationId("fb-it-4", "prof-it-4", "ALTA");
        given()
                .when()
                .get("/notifications/" + id)
                .then()
                .statusCode(200)
                .body("status", equalTo("FALHA"));
    }

    private void clearTable() {
        var items = dynamoDbClient.scan(ScanRequest.builder().tableName(TABLE_NAME).build()).items();
        for (Map<String, AttributeValue> item : items) {
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of("id", item.get("id")))
                    .build());
        }
    }

    private String notificationId(String feedbackId, String professorId, String urgencia) {
        String raw = feedbackId + "|" + professorId + "|" + urgencia;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
