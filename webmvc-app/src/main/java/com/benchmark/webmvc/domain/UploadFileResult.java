package com.benchmark.webmvc.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileResult {
    private String fileId;
    private String fileName;
    private String status;
}
