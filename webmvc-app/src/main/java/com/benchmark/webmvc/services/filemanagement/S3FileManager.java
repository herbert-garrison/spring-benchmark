package com.benchmark.webmvc.services.filemanagement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benchmark.webmvc.domain.UploadFileResult;
import com.benchmark.webmvc.properties.S3Property;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileManager implements FileManager {
    private final S3Client s3Client;
    private final S3Property s3Properties;

    private static final String FILE_GROUP = "uploads";
    private static final int UPLOAD_PART_SIZE = 5 * 1024 * 1024; // 5 MB is minimum size for S3 multipart upload

    @Override
    public UploadFileResult uploadFile(String fileName, InputStream fileContent) {
        String key = FILE_GROUP + "/" + UUID.randomUUID() + "-" + fileName;
        String bucket = this.s3Properties.getBucket();
        String uploadId = this.s3Client.createMultipartUpload(
            CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        ).uploadId();

        List<CompletedPart> parts = new ArrayList<>();

        try {
            byte[] buf = new byte[UPLOAD_PART_SIZE];
            int partNumber = 1;
            int readBytes = 0;

            while ((readBytes = fileContent.readNBytes(buf, 0, buf.length)) > 0) {
                // VT is parking here while S3 receives data
                UploadPartResponse uploadResponse = this.s3Client.uploadPart(
                    UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long)readBytes)
                        .build(),
                    RequestBody.fromInputStream(
                       new ByteArrayInputStream(buf, 0, readBytes),
                       readBytes
                    )
                );

                parts.add(CompletedPart.builder()
                    .partNumber(partNumber++)
                    .eTag(uploadResponse.eTag())
                    .build()
                );
            }
        } catch (IOException e) {
            this.abortUpload(key, bucket, uploadId);
            return UploadFileResult.builder()
                .fileId(key)
                .fileName(fileName)
                .status("FAILED")
                .build();
        }

        this.s3Client.completeMultipartUpload(
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
        );

        return UploadFileResult.builder()
            .fileId(key)
            .fileName(fileName)
            .status("UPLOADED")
            .build();
    }

    private void abortUpload(String key, String bucket, String uploadId) {
        try {
            this.s3Client.abortMultipartUpload(
                AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to abort upload {}", uploadId, e);
        }
    }
}
