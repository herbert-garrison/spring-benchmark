package com.benchmark.webflux.services.encryption;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.DataBuffer.ByteBufferIterator;
import org.springframework.stereotype.Service;

import io.netty.buffer.ByteBufAllocator;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class AesEncryptionEngine implements EncryptionEngine {
    private final SecureRandom randomizer = new SecureRandom();
    private final DataBufferFactory bufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    // Use AES Counter Mode with no padding: it is better for data streaming
    private static final String ALGORITHM = "AES/CTR/NoPadding";

    @Override
    public byte[] generateIv() {
        byte[] iv = new byte[16];
        this.randomizer.nextBytes(iv);
        return iv;
    }

    @Override
    public SecretKey generateKey() {
       try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, this.randomizer);
            return kg.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Flux<DataBuffer> wrapWithCipher(Flux<DataBuffer> in, SecretKey key, byte[] iv, int mode) {
        return Flux.defer(() -> {
            try {
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(mode, key, new IvParameterSpec(iv));

                return in
                    .publishOn(Schedulers.parallel())
                    .map(buffer -> {
                        int totalReadable = buffer.readableByteCount();
                        ByteBuffer output = ByteBuffer.allocate(cipher.getOutputSize(totalReadable));

                        try (ByteBufferIterator iterator = buffer.readableByteBuffers()) {
                            while (iterator.hasNext()) {
                                ByteBuffer input = iterator.next();
                                cipher.update(input, output);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Cipher update failed", e);
                        } finally {
                            DataBufferUtils.release(buffer);
                        }

                        output.flip();

                        return this.bufferFactory.wrap(output);
                    });
            } catch (Exception e) {
                return Flux.error(new RuntimeException("Cipher initialization failed", e));
            }
        });
    }
}
