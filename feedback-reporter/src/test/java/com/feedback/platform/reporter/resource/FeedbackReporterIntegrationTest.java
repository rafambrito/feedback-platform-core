package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.integration.DynamoDbLocalResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(DynamoDbLocalResource.class)
@TestProfile(NoAuthTestProfile.class)
class FeedbackReporterIntegrationTest {

    private static final String TABLE_NAME = "FeedbackTable";

    @Inject
    DynamoDbClient dynamoDbClient;

    @BeforeEach
    void setup() {
        clearTable();
    }

    @Test
    void weeklyReport_considersOnlyLastSevenDays_andAggregatesByProfessor() {
        Instant now = Instant.now();
        seedFeedback("it-fb-1", "curso-it", "aluno-1", "prof-a", 2, "ALTA", now.minus(1, ChronoUnit.DAYS));
        seedFeedback("it-fb-2", "curso-it", "aluno-2", "prof-a", 6, "MEDIA", now.minus(2, ChronoUnit.DAYS));
        seedFeedback("it-fb-3", "curso-it", "aluno-3", "prof-b", 8, "BAIXA", now.minus(3, ChronoUnit.DAYS));
        seedFeedback("it-fb-old", "curso-it", "aluno-4", "prof-z", 10, "BAIXA", now.minus(8, ChronoUnit.DAYS));

        given()
                .when()
                .get("/reports/semanal?cursoId=curso-it")
                .then()
                .statusCode(200)
                .body("cursoId", equalTo("curso-it"))
                .body("totalFeedbacks", equalTo(3))
                .body("mediaNota", equalTo(5.3333335f))
                .body("quantidadeAlta", equalTo(1))
                .body("quantidadeMedia", equalTo(1))
                .body("quantidadeBaixa", equalTo(1))
                .body("feedbacksByProfessor.prof-a", equalTo(2))
                .body("feedbacksByProfessor.prof-b", equalTo(1));
    }

    @Test
    void weeklyReport_withProfessorFilter_returnsOnlyProfessorData() {
        Instant now = Instant.now();
        seedFeedback("it-fb-4", "curso-it-2", "aluno-1", "prof-only", 4, "MEDIA", now.minus(1, ChronoUnit.DAYS));
        seedFeedback("it-fb-5", "curso-it-2", "aluno-2", "prof-only", 2, "ALTA", now.minus(2, ChronoUnit.DAYS));
        seedFeedback("it-fb-6", "curso-it-2", "aluno-3", "prof-other", 9, "BAIXA", now.minus(2, ChronoUnit.DAYS));

        given()
                .when()
                .get("/reports/semanal?cursoId=curso-it-2&professorId=prof-only")
                .then()
                .statusCode(200)
                .body("cursoId", equalTo("curso-it-2"))
                .body("professorId", equalTo("prof-only"))
                .body("totalFeedbacks", equalTo(2))
                .body("mediaNota", equalTo(3.0f))
                .body("feedbacksByProfessor.prof-only", equalTo(2));
    }

    @Test
    void reports_forNonexistentCursoAndProfessor_returnEmptyCollections() {
        given()
                .when()
                .get("/reports/curso/curso-inexistente")
                .then()
                .statusCode(200)
                .body("cursoId", equalTo("curso-inexistente"))
                .body("totalFeedbacks", equalTo(0));

        given()
                .when()
                .get("/reports/professor/prof-inexistente")
                .then()
                .statusCode(200)
                .body("professorId", equalTo("prof-inexistente"))
                .body("totalFeedbacks", equalTo(0));
    }

    private void seedFeedback(
            String id,
            String cursoId,
            String alunoId,
            String professorId,
            int nota,
            String criticidade,
            Instant dataCriacao) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(Map.of(
                        "id", AttributeValue.builder().s(id).build(),
                        "cursoId", AttributeValue.builder().s(cursoId).build(),
                        "alunoId", AttributeValue.builder().s(alunoId).build(),
                        "professorId", AttributeValue.builder().s(professorId).build(),
                        "nota", AttributeValue.builder().n(Integer.toString(nota)).build(),
                        "comentario", AttributeValue.builder().s("comentario-" + id).build(),
                        "criticidade", AttributeValue.builder().s(criticidade).build(),
                        "dataCriacao", AttributeValue.builder().s(dataCriacao.toString()).build()
                ))
                .build());
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
}
