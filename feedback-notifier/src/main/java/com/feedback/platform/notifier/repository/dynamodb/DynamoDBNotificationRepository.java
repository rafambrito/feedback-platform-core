package com.feedback.platform.notifier.repository.dynamodb;

import com.feedback.platform.notifier.domain.Notificacao;
import com.feedback.platform.notifier.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class DynamoDBNotificationRepository implements NotificationRepository {

    private static final Logger LOG = Logger.getLogger(DynamoDBNotificationRepository.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public DynamoDBNotificationRepository(
            DynamoDbClient dynamoDbClient,
            @ConfigProperty(name = "aws.dynamodb.notification-table", defaultValue = "NotificacaoTable")
            String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public Notificacao salvarSeAusente(Notificacao notificacao) {
        Map<String, AttributeValue> item = mapearNotificacaoParaItem(notificacao);

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .conditionExpression("attribute_not_exists(#id)")
                .expressionAttributeNames(Map.of("#id", "id"))
                .build();

        try {
            dynamoDbClient.putItem(request);
            LOG.infof("Notificação %s persistida em DynamoDB", notificacao.id());
            return notificacao;
        } catch (ConditionalCheckFailedException e) {
            LOG.infof("Notificação idempotente já existe: %s", notificacao.id());
            return buscarPorId(notificacao.id());
        } catch (DynamoDbException e) {
            LOG.errorf(e, "Erro ao persistir notificação %s em DynamoDB", notificacao.id());
            throw new RuntimeException("Falha ao persistir notificação", e);
        }
    }

    @Override
    public Notificacao buscarPorId(String id) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.builder().s(id).build()))
                .build();

        try {
            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.item().isEmpty()) {
                LOG.warnf("Notificação %s não encontrada", id);
                return null;
            }
            return mapearItemParaNotificacao(response.item());
        } catch (DynamoDbException e) {
            LOG.errorf(e, "Erro ao buscar notificação %s", id);
            throw new RuntimeException("Falha ao buscar notificação", e);
        }
    }

    @Override
    public void atualizarStatus(String id, Notificacao.StatusNotificacao status) {
        UpdateItemRequest.Builder requestBuilder = UpdateItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("id", AttributeValue.builder().s(id).build()));

        if (status == Notificacao.StatusNotificacao.ENVIADA) {
            requestBuilder
                .updateExpression("SET #status = :status, #dataAtualizacao = :dataAtualizacao, #dataEnvio = :dataEnvio")
                .expressionAttributeNames(Map.of(
                    "#status", "status",
                    "#dataAtualizacao", "dataAtualizacao",
                    "#dataEnvio", "dataEnvio"
                ))
                .expressionAttributeValues(Map.of(
                    ":status", AttributeValue.builder().s(status.name()).build(),
                    ":dataAtualizacao", AttributeValue.builder().s(Instant.now().toString()).build(),
                    ":dataEnvio", AttributeValue.builder().s(Instant.now().toString()).build()
                ));
        } else {
            requestBuilder
                .updateExpression("SET #status = :status, #dataAtualizacao = :dataAtualizacao")
                .expressionAttributeNames(Map.of(
                    "#status", "status",
                    "#dataAtualizacao", "dataAtualizacao"
                ))
                .expressionAttributeValues(Map.of(
                    ":status", AttributeValue.builder().s(status.name()).build(),
                    ":dataAtualizacao", AttributeValue.builder().s(Instant.now().toString()).build()
                ));
        }

        UpdateItemRequest request = requestBuilder.build();

        try {
            dynamoDbClient.updateItem(request);
            LOG.infof("Status da notificação %s atualizado para %s", id, status.name());
        } catch (DynamoDbException e) {
            LOG.errorf(e, "Erro ao atualizar status da notificação %s", id);
            throw new RuntimeException("Falha ao atualizar status", e);
        }
    }

    private Map<String, AttributeValue> mapearNotificacaoParaItem(Notificacao notificacao) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(notificacao.id()).build());
        item.put("feedbackId", AttributeValue.builder().s(notificacao.feedbackId()).build());
        item.put("professorId", AttributeValue.builder().s(notificacao.professorId()).build());
        item.put("alunoId", AttributeValue.builder().s(notificacao.alunoId()).build());
        item.put("urgencia", AttributeValue.builder().s(notificacao.urgencia()).build());
        item.put("email", AttributeValue.builder().s(notificacao.email()).build());
        item.put("assunto", AttributeValue.builder().s(notificacao.assunto()).build());
        item.put("corpo", AttributeValue.builder().s(notificacao.corpo()).build());
        item.put("status", AttributeValue.builder().s(notificacao.status().name()).build());
        item.put("dataCriacao", AttributeValue.builder().s(notificacao.dataCriacao().toString()).build());
        if (notificacao.dataEnvio() != null) {
            item.put("dataEnvio", AttributeValue.builder().s(notificacao.dataEnvio().toString()).build());
        }
        return item;
    }

    private Notificacao mapearItemParaNotificacao(Map<String, AttributeValue> item) {
        return new Notificacao(
                item.get("id").s(),
                item.get("feedbackId").s(),
                item.get("professorId").s(),
                item.get("alunoId").s(),
                item.get("urgencia").s(),
                item.get("email").s(),
                item.get("assunto").s(),
                item.get("corpo").s(),
                Notificacao.StatusNotificacao.valueOf(item.get("status").s()),
                Instant.parse(item.get("dataCriacao").s()),
                item.containsKey("dataEnvio") ? Instant.parse(item.get("dataEnvio").s()) : null
        );
    }
}
