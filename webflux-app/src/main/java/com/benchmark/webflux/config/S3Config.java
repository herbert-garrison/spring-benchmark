package com.benchmark.webflux.config;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.benchmark.webflux.properties.S3Property;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
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
    public S3AsyncClient s3Client() {
        return S3AsyncClient.builder()
            .endpointOverride(URI.create(this.s3Property.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(this.s3Property.getAccessKey(), this.s3Property.getSecretKey()))
            )
            .region(Region.of(this.s3Property.getRegion()))
            .forcePathStyle(true)
            .httpClientBuilder(AwsCrtAsyncHttpClient.builder()
                .maxConcurrency(200)
                .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                .connectionTimeout(Duration.ofSeconds(10))
            )
            .build();
    }

    // Mock for load testing
    @Bean
    @Primary
    public S3AsyncClient mockS3Client() {
        return new S3AsyncClient() {
            @Override
            public CompletableFuture<CreateMultipartUploadResponse> createMultipartUpload(CreateMultipartUploadRequest req) {
                int delay = RNG.nextInt(5, 20 + 1);

                return Mono.delay(Duration.ofMillis(delay))
                    .thenReturn(
                        CreateMultipartUploadResponse.builder()
                            .uploadId("mock-id")
                            .build()
                    )
                    .toFuture();
            }

            @Override
            public CompletableFuture<UploadPartResponse> uploadPart(UploadPartRequest req, AsyncRequestBody body) {
                int delay = RNG.nextInt(10, 50 + 1);

                // Just simulate byte consumation
                return Flux.from(body)
                    .doOnNext(buffer -> {})
                    .then(
                        Mono.delay(Duration.ofMillis(delay))
                            .thenReturn(
                                UploadPartResponse.builder()
                                    .eTag("mock-etag-" + req.partNumber())
                                    .build()
                            )
                    )
                    .toFuture();
            };

            @Override
            public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(CompleteMultipartUploadRequest req) {
                int delay = RNG.nextInt(5, 20 + 1);

                return Mono.delay(Duration.ofMillis(delay))
                    .thenReturn(
                        CompleteMultipartUploadResponse.builder()
                            .key(req.key())
                            .build()
                    )
                    .toFuture();
            }

            @Override
            public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUpload(AbortMultipartUploadRequest req) {
                int delay = RNG.nextInt(5, 20 + 1);

                return Mono.delay(Duration.ofMillis(delay))
                    .thenReturn(
                        AbortMultipartUploadResponse.builder()
                            .build()
                    )
                    .toFuture();
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
