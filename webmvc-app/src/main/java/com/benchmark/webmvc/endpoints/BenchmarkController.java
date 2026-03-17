package com.benchmark.webmvc.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.benchmark.webmvc.domain.CreateOrderOptions;
import com.benchmark.webmvc.domain.OrderInfo;
import com.benchmark.webmvc.domain.UploadFileResult;
import com.benchmark.webmvc.dto.CreateOrderRequestDto;
import com.benchmark.webmvc.dto.OrderInfoResponseDto;
import com.benchmark.webmvc.dto.UploadFileResponseDto;
import com.benchmark.webmvc.services.encryption.EncryptionEngine;
import com.benchmark.webmvc.services.events.SseNotifier;
import com.benchmark.webmvc.services.filemanagement.FileManager;
import com.benchmark.webmvc.services.orders.OrderManager;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BenchmarkController {
    private final EncryptionEngine encryptionEngine;
    private final FileManager fileManager;
    private final OrderManager orderManager;
    private final SseNotifier sseNotifier;

    // Case A (Regular upload)
    @PostMapping(
        value = "/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadFileResponseDto> upload(
        @RequestParam("file")
        MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .build();
        }

        InputStream rawFileContent = file.getInputStream();

        UploadFileResult uploadResult = this.fileManager.uploadFile(file.getOriginalFilename(), rawFileContent);
        UploadFileResponseDto resultBody = UploadFileResponseDto.fromUploadFileResult(uploadResult);

        return ResponseEntity.ok(resultBody);
    }

    // Case B (Upload with CPU-bound task like encryption)
    @PostMapping(
        value = "/upload-encrypt",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadFileResponseDto> uploadWithEncryption(
        @RequestParam("file")
        MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .build();
        }

        SecretKey key = this.encryptionEngine.generateKey();
        byte[] iv = this.encryptionEngine.generateIv();
        InputStream rawFileContent = file.getInputStream();
        InputStream encryptedFileContent = this.encryptionEngine.wrapWithCipher(
            rawFileContent,
            key,
            iv,
            Cipher.ENCRYPT_MODE
        );


        UploadFileResult uploadResult = this.fileManager.uploadFile(file.getOriginalFilename(), encryptedFileContent);
        UploadFileResponseDto resultBody = UploadFileResponseDto.fromUploadFileResult(uploadResult);

        return ResponseEntity.ok(resultBody);
    }

    // Case C (SSE streaming)
    @GetMapping(
        value = "/events/{userId}",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public SseEmitter subscribe(
        @PathVariable("userId")
        String userId
    ) {
        return this.sseNotifier.createConnection(userId);
    }

    // Case D (Regular business logic)
    @PostMapping(
        value = "/orders",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public OrderInfoResponseDto createOrder(
        @RequestBody
        CreateOrderRequestDto body
    ) {
        CreateOrderOptions options = CreateOrderOptions.fromCreateOrderRequestDto(body);

        OrderInfo order = this.orderManager.createOrder(options);
        OrderInfoResponseDto resultBody = OrderInfoResponseDto.fromOrderInfo(order);

        return resultBody;
    }

    @GetMapping(
        value = "/orders/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public OrderInfoResponseDto getOrder(
        @PathVariable("id")
        Long id
    ) {
        OrderInfo order = this.orderManager.getOrder(id);
        OrderInfoResponseDto resultBody = OrderInfoResponseDto.fromOrderInfo(order);

        return resultBody;
    }

    @GetMapping(
        value = "/orders/user/{userId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public List<OrderInfoResponseDto> getUserOrders(
        @PathVariable("userId")
        String userId
    ) {
        List<OrderInfo> orders = this.orderManager.getUserOrders(userId);
        List<OrderInfoResponseDto> resultBody = orders.stream()
            .map(OrderInfoResponseDto::fromOrderInfo)
            .toList();

        return resultBody;
    }

    @PostMapping(
        value = "/orders/process",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public OrderInfoResponseDto processOrder(
        @RequestBody
        CreateOrderRequestDto body
    ) {
        CreateOrderOptions options = CreateOrderOptions.fromCreateOrderRequestDto(body);

        OrderInfo order = this.orderManager.processOrderWithMultipleIo(options);
        OrderInfoResponseDto resultBody = OrderInfoResponseDto.fromOrderInfo(order);

        return resultBody;
    }

    // Utilities
    @GetMapping(
        value = "/health",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getHealthMetrics() {
        return Map.of(
            "status", "UP",
            "sseConnections", this.sseNotifier.size(),
            "isVirtual", Thread.currentThread().isVirtual()
        );
    }

    @GetMapping(
        value = "/thread-info",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getThreadInfo() {
        Thread t = Thread.currentThread();
        return Map.of(
            "name", t.getName(),
            "isVirtual", t.isVirtual(),
            "id", t.threadId()
        );
    }
}
