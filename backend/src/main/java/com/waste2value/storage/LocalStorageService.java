package com.waste2value.storage;

import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize storage directory", e);
            throw new IllegalStateException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds maximum limit of 5MB");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file type: ." + extension + ". Allowed types: jpg, jpeg, png, webp");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Invalid content type: " + contentType + ". Allowed: image/jpeg, image/png, image/webp");
        }

        String storedFilename = UUID.randomUUID().toString() + "." + extension;

        try {
            Path targetDir = subDirectory != null && !subDirectory.isBlank()
                    ? this.rootLocation.resolve(subDirectory).normalize()
                    : this.rootLocation;

            Files.createDirectories(targetDir);

            Path destinationFile = targetDir.resolve(storedFilename).normalize().toAbsolutePath();

            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new BadRequestException("Cannot store file outside current directory");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return subDirectory != null && !subDirectory.isBlank()
                    ? subDirectory + "/" + storedFilename
                    : storedFilename;

        } catch (IOException e) {
            log.error("Failed to store file {}", originalFilename, e);
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new BadRequestException("Cannot access file outside current directory");
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Could not read file: " + filename);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (file.startsWith(rootLocation)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", filename, e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
