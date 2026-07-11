package com.feedback.platform.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClientBuilder;

import java.net.URI;

@ApplicationScoped
public class AWSConfig {

    @ConfigProperty(name = "aws.dynamodb.endpoint.override", defaultValue = "")
    String dynamoDbEndpointOverride;

    @ConfigProperty(name = "aws.eventbridge.endpoint.override", defaultValue = "")
    String eventBridgeEndpointOverride;

    private Region awsRegion() {
        return Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));
    }

    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .credentialsProvider(credentialsProvider())
                .region(awsRegion());

        if (!dynamoDbEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(dynamoDbEndpointOverride));
        }

        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public EventBridgeClient eventBridgeClient() {
        EventBridgeClientBuilder builder = EventBridgeClient.builder()
                .credentialsProvider(credentialsProvider())
                .region(awsRegion());

        if (!eventBridgeEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(eventBridgeEndpointOverride));
        }

        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public AwsCredentialsProvider credentialsProvider() {
        if (!dynamoDbEndpointOverride.isBlank() || !eventBridgeEndpointOverride.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        }
        return DefaultCredentialsProvider.create();
    }
}
