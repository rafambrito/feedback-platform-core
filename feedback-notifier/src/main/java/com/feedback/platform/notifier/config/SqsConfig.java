package com.feedback.platform.notifier.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@ApplicationScoped
public class SqsConfig {

    @ConfigProperty(name = "aws.sqs.region", defaultValue = "us-east-1")
    String sqsRegion;

    @ConfigProperty(name = "aws.sqs.endpoint-override", defaultValue = "")
    String sqsEndpointOverride;

    @Produces
    @ApplicationScoped
    public AwsCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    @Produces
    @ApplicationScoped
    public SqsClient sqsClient(AwsCredentialsProvider awsCredentialsProvider) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(sqsRegion))
                .credentialsProvider(awsCredentialsProvider);

        if (!sqsEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(sqsEndpointOverride));
        }

        return builder.build();
    }
}
