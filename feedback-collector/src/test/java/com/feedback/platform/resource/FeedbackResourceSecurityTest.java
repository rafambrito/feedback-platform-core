package com.feedback.platform.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class FeedbackResourceSecurityTest {

    @Test
    void postFeedback_semToken_retorna401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cursoId": "CURSO-SEC",
                          "alunoId": "ALUNO-SEC",
                          "professorId": "PROF-SEC",
                          "nota": 7,
                          "comentario": "Tentativa sem token"
                        }
                        """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(401)
                .body("error_code", org.hamcrest.Matchers.equalTo("TOKEN_INVALID"));
    }
}
