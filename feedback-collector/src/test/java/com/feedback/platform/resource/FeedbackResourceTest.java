package com.feedback.platform.resource;

import com.feedback.platform.domain.Feedback;
import com.feedback.platform.event.EventPublisher;
import com.feedback.platform.integration.DynamoDbLocalResource;
import com.feedback.platform.repository.FeedbackRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Teste de integração do fluxo completo: HTTP → Service → DynamoDB Local.
 *
 * O DynamoDbLocalResource inicia o container ANTES do boot do Quarkus e
 * injeta o endpoint do container como 'aws.dynamodb.endpoint.override'.
 * O EventBridgeClient é mockado para evitar qualquer chamada real à AWS.
 * O EventPublisher é mockado para verificar a regra de publicação crítica.
 */
@QuarkusTest
@QuarkusTestResource(DynamoDbLocalResource.class)
class FeedbackResourceTest {

    /** Substitui EventBridgeEventPublisher — permite verificação via Mockito.
     *  Isso também impede qualquer chamada real ao AWS EventBridge. */
    @InjectMock
    EventPublisher eventPublisher;

    /** CDI bean real — usa o DynamoDbClient apontado para o container. */
    @Inject
    FeedbackRepository feedbackRepository;

    @BeforeEach
    void resetMocks() {
        reset(eventPublisher);
    }

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

    // ------------------------------------------------------------------ //
    //  Cenário 2: nota < 3 (ALTA) → evento publicado                      //
    // ------------------------------------------------------------------ //

    @Test
    void postFeedback_notaMenorQue3_publicaEventoCritico() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cursoId": "CURSO-02",
                          "alunoId": "ALUNO-02",
                          "professorId": "PROF-02",
                          "nota": 2,
                          "comentario": "Péssima aula"
                        }
                        """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(201)
                .extract().path("id");

        assertNotNull(id);

        // Verifica que o evento foi publicado com os campos corretos
        verify(eventPublisher).publishCriticalFeedback(
                eq(id),
                eq("ALUNO-02"),
                eq("PROF-02"));
    }

    // ------------------------------------------------------------------ //
    //  Cenário 3: nota >= 3 (MEDIA/BAIXA) → nenhum evento publicado       //
    // ------------------------------------------------------------------ //

    @Test
    void postFeedback_notaMaiorOuIgual3_naoPublicaEvento() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cursoId": "CURSO-03",
                          "alunoId": "ALUNO-03",
                          "professorId": "PROF-03",
                          "nota": 5,
                          "comentario": "Aula razoável"
                        }
                        """)
                .when()
                .post("/feedback")
                .then()
                .statusCode(201);

        verify(eventPublisher, never())
                .publishCriticalFeedback(anyString(), anyString(), anyString());
    }
}
