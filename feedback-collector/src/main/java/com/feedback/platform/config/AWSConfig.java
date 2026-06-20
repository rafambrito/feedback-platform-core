package com.feedback.platform.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import java.net.URI;

@ApplicationScoped
public class AWSConfig {

    @ConfigProperty(name = "aws.dynamodb.endpoint.override", defaultValue = "")
    String dynamoDbEndpointOverride;

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
        return EventBridgeClient.builder()
                .credentialsProvider(credentialsProvider())
                .region(awsRegion())
                .build();
    }

    @Produces
    @ApplicationScoped
    public AwsCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.create();
    }
}
