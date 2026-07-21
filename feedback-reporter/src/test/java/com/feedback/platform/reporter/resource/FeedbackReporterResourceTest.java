package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.dto.ReportSemanalResponseDTO;
import com.feedback.platform.reporter.service.FeedbackReportService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoAuthTestProfile.class)
class FeedbackReporterResourceTest {

    @InjectMock
    FeedbackReportService feedbackReportService;

    @Test
    void deveRetornarRelatorioSemanalComSucesso() {
        ReportSemanalResponseDTO response = new ReportSemanalResponseDTO(
                "curso-123",
                null,
                1,
                5.0,
                1,
                0,
                0,
                Map.of("2026-07-01", 1L),
                Map.of("prof-1", 1L),
                Instant.parse("2026-07-01T10:00:00Z")
        );

        when(feedbackReportService.getRelatorioSemanalCurso("curso-123", null)).thenReturn(response);

        given()
                .when()
                .get("/reports/semanal?cursoId=curso-123")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("cursoId", equalTo("curso-123"))
                .body("totalFeedbacks", equalTo(1))
                .body("feedbacksByDay.2026-07-01", equalTo(1))
                .body("feedbacksByProfessor.prof-1", equalTo(1))
                .body("quantidadePorDia.2026-07-01", equalTo(1))
                .body("quantidadeAlta", equalTo(0));
    }

    @Test
    void deveRetornarRelatorioSemanalFiltradoPorProfessor() {
        ReportSemanalResponseDTO response = new ReportSemanalResponseDTO(
                "curso-123",
                "prof-1",
                2,
                4.5,
                0,
                1,
                1,
                Map.of("2026-07-01", 2L),
                Map.of("prof-1", 2L),
                Instant.parse("2026-07-01T10:00:00Z")
        );

        when(feedbackReportService.getRelatorioSemanalCurso("curso-123", "prof-1")).thenReturn(response);

        given()
                .when()
                .get("/reports/semanal?cursoId=curso-123&professorId=prof-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("cursoId", equalTo("curso-123"))
                .body("professorId", equalTo("prof-1"));
    }

    @Test
    void deveRetornarBadRequestQuandoCursoIdAusente() {
        given()
                .when()
                .get("/reports/semanal")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", equalTo("cursoId é obrigatório"));
    }

    @Test
    void deveRetornarBadRequestQuandoCursoIdVazio() {
        given()
                .when()
                .get("/reports/semanal?cursoId=")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", equalTo("cursoId é obrigatório"));
    }

    @Test
    void deveRetornarErroInternoQuandoServicoFalha() {
        when(feedbackReportService.getRelatorioSemanalCurso(any(), any()))
                .thenThrow(new RuntimeException("falha inesperada"));

        given()
                .when()
                .get("/reports/semanal?cursoId=curso-123")
                .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Erro ao gerar relatório semanal"));
    }
}