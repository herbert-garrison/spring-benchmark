package com.benchmark.webflux.endpoints;

import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benchmark.webflux.domain.CreateOrderOptions;
import com.benchmark.webflux.dto.CreateOrderRequestDto;
import com.benchmark.webflux.dto.OrderInfoResponseDto;
import com.benchmark.webflux.dto.UploadFileResponseDto;
import com.benchmark.webflux.services.encryption.EncryptionEngine;
import com.benchmark.webflux.services.events.SseNotifier;
import com.benchmark.webflux.services.filemanagement.FileManager;
import com.benchmark.webflux.services.orders.OrderManager;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BenchmarkController {
    private final EncryptionEngine encryptionEngine;
    private final FileManager fileManager;
    private final OrderManager orderManager;
    private final SseNotifier sseNotifier;

    // Case A (Regular upload)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UploadFileResponseDto>> upload(
            @RequestPart("file") Mono<FilePart> part) {
        return part.flatMap(file -> this.fileManager.uploadFile(file.filename(), file.content()))
                .map(result -> ResponseEntity.ok(UploadFileResponseDto.fromUploadFileResult(result)))
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    // Case B (Upload with CPU-bound task like encryption)
    @PostMapping(value = "/upload-encrypt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UploadFileResponseDto>> uploadWithEncryption(
            @RequestPart("file") Mono<FilePart> part) {
        return part.flatMap(file -> {
            SecretKey key = this.encryptionEngine.generateKey();
            byte[] iv = this.encryptionEngine.generateIv();

            Flux<DataBuffer> encryptedFileContent = this.encryptionEngine.wrapWithCipher(
                    file.content(),
                    key,
                    iv,
                    Cipher.ENCRYPT_MODE);

            return this.fileManager.uploadFile(file.filename(), encryptedFileContent);
        })
                .map(result -> ResponseEntity.ok(UploadFileResponseDto.fromUploadFileResult(result)))
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    // Case C (SSE streaming)
    @GetMapping(value = "/events/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<ServerSentEvent<Object>> subscribe(
            @PathVariable("userId") String userId) {
        return this.sseNotifier.createConnection(userId);
    }

    // Case D (Regular business logic)
    @PostMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<OrderInfoResponseDto> createOrder(
            @RequestBody CreateOrderRequestDto body) {
        CreateOrderOptions options = CreateOrderOptions.fromCreateOrderRequestDto(body);

        return this.orderManager.createOrder(options)
                .map(OrderInfoResponseDto::fromOrderInfo);
    }

    @GetMapping(value = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<OrderInfoResponseDto> getOrder(
            @PathVariable("id") Long id) {
        return this.orderManager.getOrder(id)
                .map(OrderInfoResponseDto::fromOrderInfo);
    }

    @GetMapping(value = "/orders/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<OrderInfoResponseDto> getUserOrders(
            @PathVariable("userId") String userId) {
        return this.orderManager.getUserOrders(userId)
                .map(OrderInfoResponseDto::fromOrderInfo);
    }

    @PostMapping(value = "/orders/process", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<OrderInfoResponseDto> processOrder(
            @RequestBody CreateOrderRequestDto body) {
        CreateOrderOptions options = CreateOrderOptions.fromCreateOrderRequestDto(body);

        return this.orderManager.processOrderWithMultipleIo(options)
                .map(OrderInfoResponseDto::fromOrderInfo);
    }

    // Utilities
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "sseConnections", this.sseNotifier.size(),
                "thread", Thread.currentThread().getName()));
    }

    @GetMapping(value = "/thread-info", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Map<String, Object>> threadInfo() {
        Thread t = Thread.currentThread();
        return Mono.just(Map.of(
                "name", t.getName(),
                "id", t.threadId()));
    }
}
