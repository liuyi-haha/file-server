package com.liuyi.file.remote;

import com.liuyi.file.config.FileStorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.liuyi.file.api.DownloadFileRequest;
import org.liuyi.file.api.DownloadFileResponse;
import org.liuyi.file.api.UploadFileRequest;
import org.liuyi.file.api.UploadFileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileProviderTest {

    @Autowired
    FileStorageProperties fileStorageProperties;
    @Autowired
    private FileProvider fileProvider;

    @AfterEach
    void cleanupStorageDir() throws IOException {
        Path storageDir = Paths.get(fileStorageProperties.getPath());
        if (!Files.exists(storageDir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir)) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
        Files.deleteIfExists(storageDir);
    }

    @Test
    void uploadFile_successAndStoredToDisk() {
        fileStorageProperties.init();
        UploadFileRequest request = new UploadFileRequest();
        byte[] content = "hello-file".getBytes(StandardCharsets.UTF_8);
        request.setContent(content);

        UploadFileResponse response = fileProvider.uploadFile(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getFileId());
        assertFalse(response.getFileId().isBlank());

        Path savedFile = Paths.get(fileStorageProperties.getPath(), response.getFileId());
        assertTrue(Files.exists(savedFile));
        assertDoesNotThrow(() -> assertArrayEquals(content, Files.readAllBytes(savedFile)));
    }

    @Test
    void uploadFile_tooLarge_shouldFailAndNotStore() {
        fileStorageProperties.init();
        UploadFileRequest request = new UploadFileRequest();
        request.setContent(new byte[FileProvider.MAX_FILE_SIZE_BYTES + 1]);

        UploadFileResponse response = fileProvider.uploadFile(request);

        assertFalse(response.isSuccess());
        assertEquals(UploadFileResponse.UploadErrorType.FILE_TOO_LARGE, response.getErrorType());
        assertNull(response.getFileId());

        File dir = new File(fileStorageProperties.getPath());
        assertTrue(dir.exists() && dir.isDirectory());
        assertEquals(0, Objects.requireNonNull(dir.listFiles()).length);

    }

    @Test
    void downloadFile_success() {
        fileStorageProperties.init();
        UploadFileRequest uploadRequest = new UploadFileRequest();
        byte[] content = "download-me".getBytes(StandardCharsets.UTF_8);
        uploadRequest.setContent(content);
        UploadFileResponse uploadResponse = fileProvider.uploadFile(uploadRequest);
        assertTrue(uploadResponse.isSuccess());

        DownloadFileRequest request = new DownloadFileRequest();
        request.setFileId(uploadResponse.getFileId());

        DownloadFileResponse response = fileProvider.downloadFile(request);

        assertTrue(response.isSuccess());
        assertArrayEquals(content, response.getContent());
    }

    @Test
    void downloadFile_fileNotFound_shouldFail() {
        fileStorageProperties.init();
        DownloadFileRequest request = new DownloadFileRequest();
        request.setFileId("not-exist-id");

        DownloadFileResponse response = fileProvider.downloadFile(request);

        assertFalse(response.isSuccess());
        assertEquals(DownloadFileResponse.DownloadErrorType.FILE_NOT_FOUND, response.getErrorType());
        assertNull(response.getContent());
    }
}
