package com.iodsky.mysweldo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Profile("local")
@Slf4j
public class LocalStorageService extends StorageService {

    @Value("${import.upload.directory}")
    private String uploadDirectory;

    @Override
    public String store(MultipartFile file) {
        File uploadDir = new File(uploadDirectory);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IllegalStateException("Failed to create upload directory: " + uploadDirectory);
        }

        String fileName = generateKey(file.getOriginalFilename());
        try {
            Path filePath = Paths.get(uploadDirectory, fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            log.error("Failed to save uploaded file", e);
            throw new IllegalStateException("Failed to save uploaded file: " + e.getMessage(), e);
        }

        log.info("File uploaded to disk: {}", fileName);
        return fileName;
    }

    @Override
    public String store(String fileName, String contentType, byte[] bytes) {
        File uploadDir = new File(uploadDirectory);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IllegalStateException("Failed to create upload directory: " + uploadDirectory);
        }

        String key = generateKey(fileName);
        try {
            Path filePath = Paths.get(uploadDirectory, key);
            Files.write(filePath, bytes);
        } catch (Exception e) {
            log.error("Failed to store file", e);
            throw new IllegalStateException("Failed to store file: " + e.getMessage(), e);
        }

        log.info("File stored to disk: {}", key);
        return key;
    }

    @Override
    public InputStream get(String key) {
        try {
            log.info("File retrieved from disk: {}", key);
            return Files.newInputStream(Paths.get(uploadDirectory, key));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        Path filePath = Paths.get(uploadDirectory, key);
        try {
            Files.deleteIfExists(filePath);
            log.info("File deleted from disk: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to delete uploaded file: {}. Error: {}", filePath, e.getMessage(), e);
        }
    }
}
