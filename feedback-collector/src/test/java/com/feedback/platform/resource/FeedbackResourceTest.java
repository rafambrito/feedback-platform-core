package com.feedback.platform.resource;

import com.feedback.platform.domain.Feedback;
import com.feedback.platform.integration.DynamoDbLocalResource;
import com.feedback.platform.repository.FeedbackRepository;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste de integração do fluxo completo: HTTP → Service → DynamoDB Local.
 *
 * O DynamoDbLocalResource inicia o container ANTES do boot do Quarkus e
 * injeta o endpoint do container como 'aws.dynamodb.endpoint.override'.
 */
@QuarkusTest
@QuarkusTestResource(DynamoDbLocalResource.class)
@TestProfile(NoAuthTestProfile.class)
class FeedbackResourceTest {

    /** CDI bean real — usa o DynamoDbClient apontado para o container. */
    @Inject
    FeedbackRepository feedbackRepository;

    // ------------------------------------------------------------------ //
    //  Cenário 1: POST com nota normal → 201 + registro no DynamoDB       //
    // ------------------------------------------------------------------ //

    @Test
    void postFeedback_retorna201_e_persisteNoDynamoDB() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cursoId": "CURSO-01",
                          "alunoId": "ALUNO-01",
                          "professorId": "PROF-01",
                          "nota": 7,
                          "comentario": "Ótima aula"
                        }
                        """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(201)
                .extract().path("id");

        assertNotNull(id, "Resposta deve conter o id gerado");

        Feedback persisted = feedbackRepository.findById(id);
        assertNotNull(persisted, "Registro deve existir no DynamoDB");
        assertEquals("ALUNO-01", persisted.alunoId());
        assertEquals("CURSO-01", persisted.cursoId());
        assertEquals(7, persisted.nota());
    }

    @Test
    void postFeedback_payloadInvalido_retorna400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cursoId": "CURSO-03",
                          "alunoId": "ALUNO-03",
                          "professorId": "PROF-03",
                          "nota": 11,
                          "comentario": "Aula razoável"
                        }
                        """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(400)
                .body("error_code", org.hamcrest.Matchers.equalTo("BAD_REQUEST"));
    }

    @Test
    void getFeedback_inexistente_retorna404() {
        given()
                .when()
                .get("/feedback/nao-existe")
                .then()
                .statusCode(404)
                .body("error_code", org.hamcrest.Matchers.equalTo("NOT_FOUND"));
    }
}
