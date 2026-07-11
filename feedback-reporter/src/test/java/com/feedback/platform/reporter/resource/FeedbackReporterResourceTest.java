package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.dto.CursoReportResponseDTO;
import com.feedback.platform.reporter.dto.FeedbackReportItemDTO;
import com.feedback.platform.reporter.service.FeedbackReportService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class FeedbackReporterResourceTest {

    @InjectMock
    FeedbackReportService feedbackReportService;

    @Test
    void deveRetornarRelatorioSemanalComSucesso() {
        CursoReportResponseDTO response = new CursoReportResponseDTO(
                "curso-123",
                1,
                List.of(new FeedbackReportItemDTO(
                        "f-1",
                        "curso-123",
                        "aluno-1",
                        "prof-1",
                        5,
                        "Ótimo",
                        "BAIXA",
                        Instant.parse("2026-07-01T10:00:00Z")
                ))
        );

        when(feedbackReportService.getWeeklyCourseReport("curso-123")).thenReturn(response);

        given()
                .when()
                .get("/reports/weekly?courseId=curso-123")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("cursoId", equalTo("curso-123"))
                .body("totalFeedbacks", equalTo(1))
                .body("feedbacks.size()", equalTo(1));
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