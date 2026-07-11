package com.feedback.platform.reporter.resource;

import com.feedback.platform.reporter.dto.WeeklyCourseReportResponseDTO;
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
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(SecurityEnabledTestProfile.class)
class FeedbackReporterResourceSecurityTest {

    @InjectMock
    FeedbackReportService feedbackReportService;

    @Test
    void weekly_semToken_retorna401() {
        given()
                .when()
                .get("/reports/weekly?courseId=curso-123")
                .then()
                .statusCode(401)
                .body("error_code", equalTo("TOKEN_INVALID"));
    }

    @Test
    void weekly_tokenInvalido_retorna401() {
        given()
                .header("Authorization", "Bearer INVALID_TOKEN")
                .when()
                .get("/reports/weekly?courseId=curso-123")
                .then()
                .statusCode(401)
                .body("error_code", equalTo("TOKEN_INVALID"));
    }

    @Test
    void weekly_tokenValido_retorna200() {
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
                .header("Authorization", "Bearer " + SecurityEnabledTestProfile.TOKENS.generateValidToken())
                .when()
                .get("/reports/weekly?courseId=curso-123")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("courseId", equalTo("curso-123"));
    }
}
