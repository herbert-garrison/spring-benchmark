package com.benchmark.webmvc.config;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.benchmark.webmvc.properties.S3Property;

import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@Configuration
@RequiredArgsConstructor
public class S3Config {
    private final S3Property s3Property;
    private static final RandomGenerator RNG = RandomGenerator.getDefault();

    // Production-ready
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(this.s3Property.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(this.s3Property.getAccessKey(), this.s3Property.getSecretKey()))
            )
            .region(Region.of(this.s3Property.getRegion()))
            .forcePathStyle(true)
            .build();
    }

    // Mock for load testing
    @Bean
    @Primary
    public S3Client mockS3Client() {
        return new S3Client() {
            @Override
            public CreateMultipartUploadResponse createMultipartUpload(CreateMultipartUploadRequest req) {
                int delay = RNG.nextInt(5, 20 + 1);

                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return CreateMultipartUploadResponse.builder()
                    .uploadId("mock-id")
                    .build();
            }

            @Override
            public UploadPartResponse uploadPart(UploadPartRequest req, RequestBody requestBody) {
                int delay = RNG.nextInt(10, 50 + 1);

                try (InputStream is = requestBody.contentStreamProvider().newStream()) {
                    is.transferTo(OutputStream.nullOutputStream());
                    Thread.sleep(delay);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return UploadPartResponse.builder()
                    .eTag("mock-etag-" + req.partNumber())
                    .build();
            }

            @Override
            public CompleteMultipartUploadResponse completeMultipartUpload(CompleteMultipartUploadRequest req) {
                int delay = RNG.nextInt(5, 20 + 1);

                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return CompleteMultipartUploadResponse.builder()
                    .key(req.key())
                    .build();
            }

            @Override
            public AbortMultipartUploadResponse abortMultipartUpload(AbortMultipartUploadRequest req) {
                int delay = RNG.nextInt(5, 20 + 1);

                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return AbortMultipartUploadResponse.builder()
                    .build();
            }

            @Override
            public String serviceName() {
                return "s3-mock";
            }

            @Override
            public void close() {
            }
        };
    }
}
