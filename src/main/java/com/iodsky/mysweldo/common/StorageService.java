package com.iodsky.mysweldo.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class StorageService {

    public final String put(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File cannot be empty");
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".csv"))
            throw new IllegalArgumentException("Only CSV files are supported");
        return store(file);
    }

    public abstract InputStream get(String key);

    public abstract void delete(String key);

    protected abstract String store(MultipartFile file);

    protected String generateKey(String fileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return timestamp + "_" + fileName;
    }

}
