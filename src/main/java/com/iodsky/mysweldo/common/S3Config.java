package com.iodsky.mysweldo.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("prod")
public class S3Config {

    @Value("${aws.access_key}")
    private String accessKey;

    @Value("${aws.secret_access_key}")
    private String secretAccessKey;

    @Bean
    public S3Client s3Client(@Value("${aws.region}") String region) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretAccessKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
    }

}
