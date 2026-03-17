package com.benchmark.webmvc.services.filemanagement;

import java.io.InputStream;

import com.benchmark.webmvc.domain.UploadFileResult;

public interface FileManager {
    UploadFileResult uploadFile(String fileName, InputStream fileContent);
}
