package com.benchmark.webmvc.services.encryption;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.springframework.stereotype.Service;

@Service
public class AesEncryptionEngine implements EncryptionEngine {
    private final SecureRandom randomizer = new SecureRandom();

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
    public CipherInputStream wrapWithCipher(InputStream in, SecretKey key, byte[] iv, int mode) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(mode, key, new IvParameterSpec(iv));
            return new CipherInputStream(in, cipher);
        } catch (Exception e) {
            throw new RuntimeException("Cipher initialization failed", e);
        }
    }

    @Override
    public CipherOutputStream wrapWithCipher(OutputStream out, SecretKey key, byte[] iv, int mode) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(mode, key, new IvParameterSpec(iv));
            return new CipherOutputStream(out, cipher);
        } catch (Exception e) {
            throw new RuntimeException("Cipher initialization failed", e);
        }
    }
}
