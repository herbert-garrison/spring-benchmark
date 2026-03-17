package com.benchmark.webflux.services.encryption;

import javax.crypto.SecretKey;

import org.springframework.core.io.buffer.DataBuffer;

import reactor.core.publisher.Flux;

public interface EncryptionEngine {
    byte[] generateIv();

    SecretKey generateKey();

    Flux<DataBuffer> wrapWithCipher(Flux<DataBuffer> in, SecretKey key, byte[] iv, int mode);
}
