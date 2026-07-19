package com.feedback.platform.lambda.repository;

import com.feedback.platform.domain.Criticidade;
import com.feedback.platform.lambda.dto.FeedbackRequestDTO;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LambdaFeedbackRepository {

    public record SavedFeedback(
            String id,
            String cursoId,
            String alunoId,
            String professorId,
            Integer nota,
            String comentario,
            Criticidade criticidade,
            Instant dataCriacao
    ) {
    }

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public LambdaFeedbackRepository() {
        this.tableName = System.getenv().getOrDefault("AWS_DYNAMODB_TABLE", "FeedbackTable");
        this.dynamoDbClient = buildClient();
    }

    public SavedFeedback save(FeedbackRequestDTO request, String requestId) {
        String feedbackId = UUID.randomUUID().toString();
        Instant dataCriacao = Instant.now();
        Criticidade criticidade = avaliarCriticidade(request.nota());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(feedbackId).build());
        item.put("cursoId", AttributeValue.builder().s(request.cursoId()).build());
        item.put("alunoId", AttributeValue.builder().s(request.alunoId()).build());
        item.put("professorId", AttributeValue.builder().s(request.professorId()).build());
        item.put("nota", AttributeValue.builder().n(Integer.toString(request.nota())).build());
        item.put("comentario", AttributeValue.builder().s(request.comentario()).build());
        item.put("criticidade", AttributeValue.builder().s(criticidade.name()).build());
        item.put("dataCriacao", AttributeValue.builder().s(dataCriacao.toString()).build());
        item.put("requestId", AttributeValue.builder().s(requestId).build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(putItemRequest);

        return new SavedFeedback(
            feedbackId,
            request.cursoId(),
            request.alunoId(),
            request.professorId(),
            request.nota(),
            request.comentario(),
            criticidade,
            dataCriacao
        );
    }

    public Map<String, AttributeValue> findById(String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(getItemRequest);
        if (response == null || response.item() == null || response.item().isEmpty()) {
            return null;
        }

        return response.item();
    }

    private Criticidade avaliarCriticidade(int nota) {
        if (nota < 3) {
            return Criticidade.ALTA;
        }
        if (nota <= 6) {
            return Criticidade.MEDIA;
        }
        return Criticidade.BAIXA;
    }

    private DynamoDbClient buildClient() {
        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        String endpointOverride = System.getenv().getOrDefault("AWS_DYNAMODB_ENDPOINT_OVERRIDE", "");

        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(Region.of(region));

        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
            return builder.build();
        }

        builder.credentialsProvider(DefaultCredentialsProvider.create());
        return builder.build();
    }
}
