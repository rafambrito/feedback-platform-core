package com.feedback.platform.reporter.repository.dynamodb;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DynamoDBFeedbackRepository implements FeedbackRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public DynamoDBFeedbackRepository(
            DynamoDbClient dynamoDbClient,
            @ConfigProperty(name = "aws.dynamodb.table-name", defaultValue = "FeedbackTable") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public List<FeedbackRecord> findByProfessorId(String professorId) {
        return scanByAttribute("professorId", professorId);
    }

    @Override
    public List<FeedbackRecord> findByCursoId(String cursoId) {
        return scanByAttribute("cursoId", cursoId);
    }

    private List<FeedbackRecord> scanByAttribute(String attributeName, String value) {
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#attr", attributeName);

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":value", AttributeValue.builder().s(value).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("#attr = :value")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse response = dynamoDbClient.scan(request);

        List<FeedbackRecord> feedbacks = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            feedbacks.add(toDomain(item));
        }

        return feedbacks;
    }

    private FeedbackRecord toDomain(Map<String, AttributeValue> item) {
        return new FeedbackRecord(
                getString(item, "id"),
                getString(item, "cursoId"),
                getString(item, "alunoId"),
                getString(item, "professorId"),
                getInt(item, "nota"),
                getString(item, "comentario"),
                getString(item, "criticidade"),
                getInstant(item, "dataCriacao")
        );
    }

    private String getString(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null && value.s() != null ? value.s() : "";
    }

    private Integer getInt(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value == null || value.n() == null || value.n().isBlank()) {
            return null;
        }
        return Integer.parseInt(value.n());
    }

    private Instant getInstant(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value == null || value.s() == null || value.s().isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value.s());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
