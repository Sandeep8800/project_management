package com.nexuspms.common.storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * HLD S5/S10 local-disk stand-in for the S3-compatible object storage the
 * design docs specify. AttachmentService.generateUploadUrl points clients
 * here instead of a real cloud provider -- swapping to one later means
 * replacing this class and that one method, nothing else (the two-step
 * pre-signed-URL contract in API Design S6 stays identical either way).
 *
 * Security note: this endpoint requires authentication (no permitAll rule for
 * it in SecurityConfig) but does NOT perform per-issue membership checks the
 * way REST endpoints do -- authorization relies on storageKey containing an
 * unguessable random UUID component, the same posture a real pre-signed URL
 * has. Acceptable for this pass's stand-in; a real S3 provider's pre-signed
 * URLs would have the same property. Path traversal is blocked by resolving
 * every key against the base directory and rejecting anything that escapes it.
 */
@RestController
@RequestMapping("/storage")
public class LocalObjectStorageController {

    private final Path baseDir;

    public LocalObjectStorageController(@Value("${nexus.storage.local-path}") String localPath) throws IOException {
        this.baseDir = Path.of(localPath).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
    }

    @PutMapping("/{*storageKey}")
    public void upload(@PathVariable String storageKey, HttpServletRequest request) throws IOException {
        Path target = resolveWithinBaseDir(storageKey);
        Files.createDirectories(target.getParent());
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @GetMapping("/{*storageKey}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String storageKey) {
        Path target = resolveWithinBaseDir(storageKey);
        if (!Files.exists(target)) {
            throw new com.nexuspms.common.exception.ResourceNotFoundException("No such object: " + storageKey);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(target));
    }

    private Path resolveWithinBaseDir(String rawKey) {
        String key = rawKey.startsWith("/") ? rawKey.substring(1) : rawKey;
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new AccessDeniedException("Invalid storage key.");
        }
        return resolved;
    }
}
