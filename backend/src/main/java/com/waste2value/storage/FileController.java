package com.waste2value.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Storage", description = "Public endpoints for serving uploaded listing images")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{filename:.+}")
    @Operation(summary = "View/Download an uploaded image")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);

        String contentType = "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if (lower.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (lower.endsWith(".webp")) {
            contentType = "image/webp";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(file);
    }
}
