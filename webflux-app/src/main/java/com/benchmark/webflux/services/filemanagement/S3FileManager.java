package com.benchmark.webflux.services.filemanagement;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;

import com.benchmark.webflux.domain.UploadFileResult;
import com.benchmark.webflux.properties.S3Property;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileManager implements FileManager {
    private final S3AsyncClient s3Client;
    private final S3Property s3Properties;

    private static final String FILE_GROUP = "uploads";
    private static final int UPLOAD_PART_SIZE = 5 * 1024 * 1024; // 5 MB is minimum size for S3 multipart upload

    @Override
    public Mono<UploadFileResult> uploadFile(String fileName, Flux<DataBuffer> fileContent) {
        String key = FILE_GROUP + "/" + UUID.randomUUID() + "-" + fileName;
        String bucket = this.s3Properties.getBucket();

        AtomicLong sizeCounter = new AtomicLong(0);

        return this.initiateUpload(key, bucket)
            .flatMapMany(uploadId -> {
                return fileContent
                    .bufferUntil(data -> {
                        long bufferedBytes = sizeCounter.addAndGet(data.readableByteCount());
                        if (bufferedBytes >= UPLOAD_PART_SIZE) {
                            sizeCounter.set(0);
                            return true;
                        }

                        return false;
                    })
                    .index()
                    .flatMap(tuple -> {
                        int partNumber = tuple.getT1().intValue() + 1;
                        List<DataBuffer> buffered = tuple.getT2();

                        return this.uploadPart(key, bucket, uploadId, partNumber, buffered);
                    }, 4) // No more than 4 chunks per file concurrently
                    .collectList()
                    .flatMap(parts -> {
                        List<CompletedPart> sortedParts = parts.stream()
                            .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                            .toList();

                        return this.completeUpload(key, bucket, uploadId, sortedParts);
                    });
            })
            .then()
            .thenReturn(UploadFileResult.builder()
                .fileId(key)
                .fileName(fileName)
                .status("UPLOADED")
                .build()
            )
            .onErrorResume((e) -> this.abortUpload(key, bucket, bucket)
                .thenReturn(
                    UploadFileResult.builder()
                        .fileId(key)
                        .fileName(fileName)
                        .status("FAILED")
                        .build()
                )
            );
    }

    private Mono<String> initiateUpload(String key, String bucket) {
        return Mono.fromFuture(this.s3Client.createMultipartUpload(
            CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        ))
        .map(CreateMultipartUploadResponse::uploadId);
    }

    private Mono<Void> completeUpload(String key, String bucket, String uploadId, List<CompletedPart> parts) {
        return Mono.fromFuture(this.s3Client.completeMultipartUpload(
            CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(
                    CompletedMultipartUpload.builder()
                        .parts(parts)
                        .build()
                )
                .build()
        )).then();
    }

    private Mono<Void> abortUpload(String key, String bucket, String uploadId) {
        return Mono.fromFuture(this.s3Client.abortMultipartUpload(
            AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build()
        )).then();
    }

    private Mono<CompletedPart> uploadPart(
        String key,
        String bucket,
        String uploadId,
        int partNumber,
        List<DataBuffer> data
    ) {
        long contentLength = data.stream()
            .mapToLong(DataBuffer::readableByteCount)
            .sum();

        Flux<ByteBuffer> chunkPublisher = Flux.fromIterable(data)
            .concatMap(buffer -> Flux.fromIterable(() -> buffer.readableByteBuffers()))
            .doFinally(signal -> {
                // Manual memory release is required, otherwise it would be hold and cause memory leak
                for (DataBuffer buffer : data) {
                    DataBufferUtils.release(buffer);
                }
            });

        return Mono.fromFuture(this.s3Client.uploadPart(
            UploadPartRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength(contentLength)
                .build(),
            AsyncRequestBody.fromPublisher(chunkPublisher)
        )).map(response -> CompletedPart.builder()
            .eTag(response.eTag())
            .partNumber(partNumber)
            .build()
        );
    }
}
