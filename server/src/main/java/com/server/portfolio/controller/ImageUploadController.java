package com.server.portfolio.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
public class ImageUploadController {

    @Value("${image.upload-dir:./uploads/images}")
    private String uploadDir;

    @Value("${image.base-url:http://localhost:8081/images}")
    private String imageBaseUrl;

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "파일이 비어있습니다."));
        }

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".png";

        String fileName = UUID.randomUUID() + ext;

        try {
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path destPath = dirPath.resolve(fileName);
            file.transferTo(destPath.toFile());

            String url = imageBaseUrl + "/" + fileName;
            log.info("Image uploaded successfully to: {}", destPath.toString());
            return ResponseEntity.ok(Map.of("success", true, "url", url));
        } catch (IOException e) {
            log.error("Image upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "업로드 실패: " + e.getMessage()));
        }
    }
}
