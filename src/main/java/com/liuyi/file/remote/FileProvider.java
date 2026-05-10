package com.liuyi.file.remote;

import com.liuyi.file.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.liuyi.common.domain.object.RandomIdGenerator;
import org.liuyi.file.api.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
@DubboService
@Slf4j
@Component
public class FileProvider implements FileService {
    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private final FileStorageProperties fileStorageProperties;

    @Override
    public DownloadFileResponse downloadFile(DownloadFileRequest request) {
        DownloadFileResponse response = new DownloadFileResponse();
        if (request == null || request.getFileId() == null || request.getFileId().isBlank()) {
            throw new IllegalArgumentException("fileId cannot be null or blank");
        }

        try {
            Path filePath = Paths.get(fileStorageProperties.getPath(), request.getFileId());
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                response.setSuccess(false);
                response.setErrorType(DownloadFileResponse.DownloadErrorType.FILE_NOT_FOUND);
                return response;
            }

            var fileContent = Files.readAllBytes(filePath);
            response.setContent(fileContent);
            response.setSuccess(true);
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setErrorType(DownloadFileResponse.DownloadErrorType.UNKNOWN_ERROR);
            log.error("download file error, fileId: {}, error: {}", request.getFileId(), ex.getMessage());
        }
        return response;
    }

    @Override
    public UploadFileResponse uploadFile(UploadFileRequest request) {
        UploadFileResponse response = new UploadFileResponse();
        if (request == null || request.getContent() == null) {
            response.setSuccess(false);
            response.setErrorType(UploadFileResponse.UploadErrorType.UNKNOWN_ERROR);
            return response;
        }

        byte[] content = request.getContent();
        if (content.length > MAX_FILE_SIZE_BYTES) {
            response.setSuccess(false);
            response.setErrorType(UploadFileResponse.UploadErrorType.FILE_TOO_LARGE);
            return response;
        }

        String fileId = RandomIdGenerator.generate();
        Path filePath = Paths.get(fileStorageProperties.getPath(), fileId);
        try {
            Files.write(filePath, content);
            response.setSuccess(true);
            response.setFileId(fileId);
            return response;
        } catch (IOException ex) {
            log.error("写入文件失败", ex);
            response.setSuccess(false);
            response.setErrorType(UploadFileResponse.UploadErrorType.UNKNOWN_ERROR);
            return response;
        } catch (Exception ex) {
            log.error("upload file error, error:", ex);
            response.setSuccess(false);
            response.setErrorType(UploadFileResponse.UploadErrorType.UNKNOWN_ERROR);
            return response;
        }
    }
}
