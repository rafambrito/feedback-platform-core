package com.feedback.platform.notifier.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@ApplicationScoped
public class AWSConfig {

    @ConfigProperty(name = "aws.region", defaultValue = "us-east-1")
    String awsRegion;

    @ConfigProperty(name = "aws.dynamodb.endpoint-override", defaultValue = "")
    String dynamodbEndpointOverride;

    @ConfigProperty(name = "aws.sqs.endpoint-override", defaultValue = "")
    String sqsEndpointOverride;

    @Produces
    @ApplicationScoped
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (!dynamodbEndpointOverride.isBlank()) {
            // LocalStack: usar credenciais dummy
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")
            );
        }
        // AWS real: usar credenciais padrão
        return DefaultCredentialsProvider.create();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient(AwsCredentialsProvider credentialsProvider) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider);

        if (!dynamodbEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(dynamodbEndpointOverride));
        }

        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider);

        if (!sqsEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(sqsEndpointOverride));
        }

        return builder.build();
    }
}
