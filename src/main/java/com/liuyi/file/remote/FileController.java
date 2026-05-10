package com.liuyi.file.remote;

import com.liuyi.file.config.FileStorageProperties;
import com.liuyi.file.openapi.FilesApi;
import com.liuyi.file.openapi.UploadFile200Response;
import com.liuyi.file.openapi.UploadFile200ResponseData;
import com.liuyi.file.utils.JwtVerifier;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/file-server")
@RequiredArgsConstructor
public class FileController implements FilesApi {
    private final FileStorageProperties fileStorageProperties;
    private final JwtVerifier jwtVerifier;

    @Override
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(String fileId) {
        try {
            Path filePath = Paths.get(fileStorageProperties.getPath(), fileId);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.error("File not found: {}", filePath);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(null);
            }
            log.info("Serving file: {}", filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new UrlResource(filePath.toUri()));
        } catch (Exception ex) {
            log.error("Error loading file: {}", ex.getMessage(), ex);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(null);
        }
    }

    @Override
    public ResponseEntity<UploadFile200Response> uploadFile(String xUploadToken, MultipartFile file) {
        UploadFile200Response resp = new UploadFile200Response();
        try {
            var claims = jwtVerifier.verify(xUploadToken);
            String fileId = claims.get("file_id", String.class);
            Path filePath = Paths.get(fileStorageProperties.getPath(), fileId);
            Files.write(filePath, file.getBytes());
            UploadFile200ResponseData data = new UploadFile200ResponseData().fileId(fileId);
            resp.success(true).data(data);
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException |
                 IllegalArgumentException ex) {
            log.error("Invalid upload token: {}", ex.getMessage(), ex);
            resp.success(false).errCode(UploadFile200Response.ErrCodeEnum.INVALID_UPLOAD_TOKEN).errMsg(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error saving uploaded file", ex);
            resp.success(false).errCode(UploadFile200Response.ErrCodeEnum.UNKNOWN_ERROR).errMsg(ex.getMessage());
        }

        log.info(resp.toString());
        return ResponseEntity.ok(resp);

    }
}
