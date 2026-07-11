package com.feedback.platform.reporter.repository.dynamodb;

import com.feedback.platform.reporter.domain.FeedbackRecord;
import com.feedback.platform.reporter.repository.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class DynamoDBFeedbackRepository implements FeedbackRepository {

    private static final Logger LOG = Logger.getLogger(DynamoDBFeedbackRepository.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String cursoIndexName;
    private final String professorIndexName;

    @Inject
    public DynamoDBFeedbackRepository(
            DynamoDbClient dynamoDbClient,
            @ConfigProperty(name = "aws.dynamodb.table-name", defaultValue = "FeedbackTable") String tableName,
            @ConfigProperty(name = "aws.dynamodb.gsi.curso-name") Optional<String> cursoIndexName,
            @ConfigProperty(name = "aws.dynamodb.gsi.professor-name") Optional<String> professorIndexName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.cursoIndexName = cursoIndexName.filter(v -> !v.isBlank()).orElse("");
        this.professorIndexName = professorIndexName.filter(v -> !v.isBlank()).orElse("");
    }

    @Override
    public List<FeedbackRecord> findByProfessorId(String professorId) {
        if (!professorIndexName.isBlank()) {
            List<FeedbackRecord> queried = queryByPartitionKey(professorIndexName, "professorId", professorId, null, null);
            if (!queried.isEmpty()) {
                return queried;
            }
        }
        return scanByAttribute("professorId", professorId);
    }

    @Override
    public List<FeedbackRecord> findByCursoId(String cursoId) {
        if (!cursoIndexName.isBlank()) {
            List<FeedbackRecord> queried = queryByPartitionKey(cursoIndexName, "cursoId", cursoId, null, null);
            if (!queried.isEmpty()) {
                return queried;
            }
        }
        return scanByAttribute("cursoId", cursoId);
    }

    @Override
    public List<FeedbackRecord> findByCursoIdAndProfessorId(String cursoId, String professorId) {
        if (!cursoIndexName.isBlank()) {
            Map<String, AttributeValue> extraValues = Map.of(":professorId", AttributeValue.builder().s(professorId).build());

            List<FeedbackRecord> queried = queryByPartitionKey(
                    cursoIndexName,
                    "cursoId",
                    cursoId,
                    "#professorId = :professorId",
                    mergeValues(extraValues)
            );
            if (!queried.isEmpty()) {
                return queried;
            }
        }
        return scanByCursoAndProfessor(cursoId, professorId);
    }

    private List<FeedbackRecord> queryByPartitionKey(String indexName,
                                                     String partitionAttribute,
                                                     String partitionValue,
                                                     String extraFilterExpression,
                                                     Map<String, AttributeValue> extraValues) {
        try {
            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#pk", partitionAttribute);

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":pk", AttributeValue.builder().s(partitionValue).build());
            if (extraValues != null) {
                expressionValues.putAll(extraValues);
            }

            QueryRequest.Builder builder = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName(indexName)
                    .keyConditionExpression("#pk = :pk")
                    .expressionAttributeNames(expressionNames)
                    .expressionAttributeValues(expressionValues);

            if (extraFilterExpression != null && !extraFilterExpression.isBlank()) {
                builder.filterExpression(extraFilterExpression);
                if (extraFilterExpression.contains("#professorId")) {
                    expressionNames.put("#professorId", "professorId");
                }
                builder.expressionAttributeNames(expressionNames);
            }

            QueryResponse response = dynamoDbClient.query(builder.build());
            List<FeedbackRecord> feedbacks = new ArrayList<>();
            for (Map<String, AttributeValue> item : response.items()) {
                feedbacks.add(toDomain(item));
            }
            return feedbacks;
        } catch (DynamoDbException e) {
            LOG.warnf("Query no indice %s falhou. Usando fallback para scan. motivo=%s", indexName, e.getMessage());
            return List.of();
        }
    }

    private Map<String, AttributeValue> mergeValues(Map<String, AttributeValue> values) {
        return new HashMap<>(values);
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

    private List<FeedbackRecord> scanByCursoAndProfessor(String cursoId, String professorId) {
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#curso", "cursoId");
        expressionAttributeNames.put("#professor", "professorId");

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":curso", AttributeValue.builder().s(cursoId).build());
        expressionAttributeValues.put(":professor", AttributeValue.builder().s(professorId).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("#curso = :curso AND #professor = :professor")
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
