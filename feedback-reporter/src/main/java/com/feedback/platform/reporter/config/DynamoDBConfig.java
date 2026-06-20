package com.feedback.platform.reporter.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

@ApplicationScoped
public class DynamoDBConfig {

    @ConfigProperty(name = "aws.region", defaultValue = "us-east-1")
    String awsRegion;

    @ConfigProperty(name = "aws.dynamodb.endpoint.override", defaultValue = "")
    String dynamoDbEndpointOverride;

    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion));

        if (!dynamoDbEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(dynamoDbEndpointOverride));
            builder.credentialsProvider(localCredentialsProvider());
        }

        return builder.build();
    }

    private AwsCredentialsProvider localCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    }
}
