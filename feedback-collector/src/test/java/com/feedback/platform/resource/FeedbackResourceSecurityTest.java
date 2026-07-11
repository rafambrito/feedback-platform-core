package com.feedback.platform.resource;

import com.feedback.platform.domain.Criticidade;
import com.feedback.platform.dto.FeedbackResponseDTO;
import com.feedback.platform.service.FeedbackService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(SecurityEnabledTestProfile.class)
class FeedbackResourceSecurityTest {

  @InjectMock
  FeedbackService feedbackService;

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

                @Test
                void postFeedback_tokenInvalido_retorna401() {
              given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer INVALID_TOKEN")
                .body("""
                  {
                    "cursoId": "CURSO-SEC",
                    "alunoId": "ALUNO-SEC",
                    "professorId": "PROF-SEC",
                    "nota": 7,
                    "comentario": "Tentativa com token invalido"
                  }
                  """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(401)
                .body("error_code", org.hamcrest.Matchers.equalTo("TOKEN_INVALID"));
                }

                @Test
                void postFeedback_tokenValido_retorna201() {
              when(feedbackService.processarFeedback(any())).thenReturn(
                new FeedbackResponseDTO(
                  "fb-sec-1",
                  "CURSO-SEC",
                  "ALUNO-SEC",
                  "PROF-SEC",
                  8,
                  "Token válido",
                  Criticidade.BAIXA,
                  Instant.now()
                )
              );

              given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + SecurityEnabledTestProfile.TOKENS.generateValidToken())
                .body("""
                  {
                    "cursoId": "CURSO-SEC",
                    "alunoId": "ALUNO-SEC",
                    "professorId": "PROF-SEC",
                    "nota": 8,
                    "comentario": "Tentativa com token valido"
                  }
                  """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(201)
                .body("id", org.hamcrest.Matchers.equalTo("fb-sec-1"));
                }
}
