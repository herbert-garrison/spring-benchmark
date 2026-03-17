package com.benchmark.webflux.services.filemanagement;

import org.springframework.core.io.buffer.DataBuffer;

import com.benchmark.webflux.domain.UploadFileResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileManager {
    Mono<UploadFileResult> uploadFile(String fileName, Flux<DataBuffer> fileContent);
}
