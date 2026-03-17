package com.benchmark.webflux.dto;

import com.benchmark.webflux.domain.UploadFileResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileResponseDto {
    private String fileId;
    private String fileName;
    private String status;

    public static UploadFileResponseDto fromUploadFileResult(UploadFileResult data) {
        return UploadFileResponseDto.builder()
            .fileId(data.getFileId())
            .fileName(data.getFileName())
            .status(data.getStatus())
            .build();
    }
}
