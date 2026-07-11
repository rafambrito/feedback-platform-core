package com.feedback.platform.event;

import com.feedback.platform.integration.DynamoDbLocalResource;
import com.feedback.platform.resource.NoAuthTestProfile;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(DynamoDbLocalResource.class)
@TestProfile(NoAuthTestProfile.class)
class CriticalFeedbackPublicationTest {

    @InjectMock
    EventPublisher eventPublisher;

    @BeforeEach
    void resetMocks() {
        reset(eventPublisher);
    }

    @Test
    void urgentTrue_equivalenteANotaBaixa_publicaEventoCritico() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cursoId": "CURSO-EVT-1",
                          "alunoId": "ALUNO-EVT-1",
                          "professorId": "PROF-EVT-1",
                          "nota": 2,
                          "comentario": "Aula ruim"
                        }
                        """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(201)
                .extract().path("id");

        verify(eventPublisher).publishCriticalFeedback(eq(id), eq("ALUNO-EVT-1"), eq("PROF-EVT-1"));
    }

    @Test
    void urgentFalse_equivalenteANotaNaoCritica_naoPublicaEvento() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cursoId": "CURSO-EVT-2",
                          "alunoId": "ALUNO-EVT-2",
                          "professorId": "PROF-EVT-2",
                          "nota": 7,
                          "comentario": "Aula ok"
                        }
                        """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(201);

        verify(eventPublisher, never()).publishCriticalFeedback(anyString(), anyString(), anyString());
    }
}
