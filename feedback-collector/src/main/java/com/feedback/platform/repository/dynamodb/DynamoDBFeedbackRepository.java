package com.feedback.platform.repository.dynamodb;

import com.feedback.platform.domain.Feedback;
import com.feedback.platform.repository.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class DynamoDBFeedbackRepository implements FeedbackRepository {

    private static final String TABLE_NAME = "FeedbackTable";

    private final DynamoDbClient dynamoDbClient;

    @Inject
    public DynamoDBFeedbackRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public void save(Feedback feedback) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(feedback.id()).build());
        item.put("cursoId", AttributeValue.builder().s(feedback.cursoId()).build());
        item.put("alunoId", AttributeValue.builder().s(feedback.alunoId()).build());
        item.put("professorId", AttributeValue.builder().s(feedback.professorId()).build());
        item.put("nota", AttributeValue.builder().n(Integer.toString(feedback.nota())).build());
        item.put("comentario", AttributeValue.builder().s(feedback.comentario()).build());
        item.put("criticidade", AttributeValue.builder().s(feedback.criticidade().name()).build());
        item.put("dataCriacao", AttributeValue.builder().s(feedback.dataCriacao().toString()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    @Override
    public Feedback findById(String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        GetItemRequest get = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        GetItemResponse resp = dynamoDbClient.getItem(get);
        if (resp == null || resp.item() == null || resp.item().isEmpty()) {
            return null;
        }

        Map<String, AttributeValue> item = resp.item();

        String cid = item.getOrDefault("cursoId", AttributeValue.builder().s("").build()).s();
        String aid = item.getOrDefault("alunoId", AttributeValue.builder().s("").build()).s();
        String pid = item.getOrDefault("professorId", AttributeValue.builder().s("").build()).s();
        int nota = Integer.parseInt(item.getOrDefault("nota", AttributeValue.builder().n("0").build()).n());
        String comentario = item.getOrDefault("comentario", AttributeValue.builder().s("").build()).s();
        String crit = item.getOrDefault("criticidade", AttributeValue.builder().s("BAIXA").build()).s();
        String data = item.getOrDefault("dataCriacao", AttributeValue.builder().s("").build()).s();

        java.time.Instant instant = data.isBlank() ? java.time.Instant.now() : java.time.Instant.parse(data);

        return new Feedback(id, cid, aid, pid, nota, comentario, com.feedback.platform.domain.Criticidade.valueOf(crit), instant);
    }
}
