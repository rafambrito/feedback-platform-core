package com.feedback.platform.reporter.integration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.util.Map;

public class DynamoDbLocalResource implements QuarkusTestResourceLifecycleManager {

    private static final String IMAGE = "amazon/dynamodb-local:2.5.4";
    private static final int DYNAMODB_PORT = 8000;
    private static final String TABLE_NAME = "FeedbackTable";

    private GenericContainer<?> dynamoDb;

    @Override
    @SuppressWarnings("resource")
    public Map<String, String> start() {
        dynamoDb = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withExposedPorts(DYNAMODB_PORT)
                .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-sharedDb");

        dynamoDb.start();

        String endpoint = "http://" + dynamoDb.getHost() + ":" + dynamoDb.getMappedPort(DYNAMODB_PORT);
        createFeedbackTable(endpoint);

        return Map.of(
                "aws.dynamodb.endpoint.override", endpoint,
                "aws.dynamodb.table-name", TABLE_NAME,
                "aws.dynamodb.gsi.curso-name", "",
                "aws.dynamodb.gsi.professor-name", "",
                "security.interceptor.enabled", "false"
        );
    }

    @Override
    public void stop() {
        if (dynamoDb != null && dynamoDb.isRunning()) {
            dynamoDb.stop();
        }
    }

    private void createFeedbackTable(String endpoint) {
        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .region(Region.US_EAST_1)
                .build()) {
            client.createTable(CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        }
    }
}
