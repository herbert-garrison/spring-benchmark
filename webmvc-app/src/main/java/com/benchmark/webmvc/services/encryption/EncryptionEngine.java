package com.benchmark.webmvc.services.encryption;

import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

public interface EncryptionEngine {
    byte[] generateIv();

    SecretKey generateKey();

    CipherInputStream wrapWithCipher(InputStream in, SecretKey key, byte[] iv, int mode);

    CipherOutputStream wrapWithCipher(OutputStream out, SecretKey key, byte[] iv, int mode);
}
