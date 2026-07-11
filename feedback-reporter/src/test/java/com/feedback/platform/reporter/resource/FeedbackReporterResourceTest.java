package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.dto.WeeklyCourseReportResponseDTO;
import com.feedback.platform.reporter.service.FeedbackReportService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class FeedbackReporterResourceTest {

    @InjectMock
    FeedbackReportService feedbackReportService;

    @Test
    void deveRetornarRelatorioSemanalComSucesso() {
        WeeklyCourseReportResponseDTO response = new WeeklyCourseReportResponseDTO(
                "curso-123",
                null,
                1,
                5.0,
                1,
                0,
                0,
                Map.of("prof-1", 1L),
                Instant.parse("2026-07-01T10:00:00Z")
        );

        when(feedbackReportService.getWeeklyCourseReport("curso-123", null)).thenReturn(response);

        given()
                .when()
                .get("/reports/weekly?courseId=curso-123")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("courseId", equalTo("curso-123"))
                .body("totalFeedbacks", equalTo(1))
                .body("altaCount", equalTo(0));
    }

    @Test
    void deveRetornarRelatorioSemanalFiltradoPorProfessor() {
        WeeklyCourseReportResponseDTO response = new WeeklyCourseReportResponseDTO(
                "curso-123",
                "prof-1",
                2,
                4.5,
                0,
                1,
                1,
                Map.of("prof-1", 2L),
                Instant.parse("2026-07-01T10:00:00Z")
        );

        when(feedbackReportService.getWeeklyCourseReport("curso-123", "prof-1")).thenReturn(response);

        given()
                .when()
                .get("/reports/weekly?courseId=curso-123&professorId=prof-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("courseId", equalTo("curso-123"))
                .body("professorId", equalTo("prof-1"));
    }

    @Test
    void deveRetornarBadRequestQuandoCourseIdAusente() {
        given()
                .when()
                .get("/reports/weekly")
                .then()
                .statusCode(400);
    }

    @Test
    void deveRetornarBadRequestQuandoCourseIdVazio() {
        given()
                .when()
                .get("/reports/weekly?courseId=")
                .then()
                .statusCode(400)
                .body(equalTo("courseId é obrigatório"));
    }
}