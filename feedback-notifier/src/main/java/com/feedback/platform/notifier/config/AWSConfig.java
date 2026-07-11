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
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class AWSConfig {

    @ConfigProperty(name = "aws.region", defaultValue = "us-east-1")
    String awsRegion;

    @ConfigProperty(name = "aws.dynamodb.endpoint-override")
    Optional<String> dynamodbEndpointOverride;

    @ConfigProperty(name = "aws.sqs.endpoint-override")
    Optional<String> sqsEndpointOverride;

    @ConfigProperty(name = "aws.ses.endpoint-override")
    Optional<String> sesEndpointOverride;

    @Produces
    @ApplicationScoped
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (dynamodbEndpointOverride.filter(v -> !v.isBlank()).isPresent()
            || sqsEndpointOverride.filter(v -> !v.isBlank()).isPresent()
            || sesEndpointOverride.filter(v -> !v.isBlank()).isPresent()) {
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

        dynamodbEndpointOverride.filter(v -> !v.isBlank())
            .ifPresent(value -> builder.endpointOverride(URI.create(value)));

        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider);

        sqsEndpointOverride.filter(v -> !v.isBlank())
            .ifPresent(value -> builder.endpointOverride(URI.create(value)));

        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public SesClient sesClient(AwsCredentialsProvider credentialsProvider) {
        SesClientBuilder builder = SesClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider);

        sesEndpointOverride.filter(v -> !v.isBlank())
            .ifPresent(value -> builder.endpointOverride(URI.create(value)));

        return builder.build();
    }
}
