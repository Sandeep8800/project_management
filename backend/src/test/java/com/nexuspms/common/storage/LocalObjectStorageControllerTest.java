package com.nexuspms.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Path-traversal protection is the one piece of this stand-in storage backend that's actually security-sensitive -- worth pinning directly. */
class LocalObjectStorageControllerTest {

    @TempDir
    Path tempDir;

    private LocalObjectStorageController controller;

    @BeforeEach
    void setUp() throws IOException {
        controller = new LocalObjectStorageController(tempDir.toString());
    }

    @Test
    void download_withParentDirectoryTraversal_isRejected() {
        assertThatThrownBy(() -> controller.download("../../../../etc/passwd"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void download_withEncodedTraversalAttempt_isRejectedOrNotFound() {
        // A key that normalizes back inside the base dir after resolving ".." segments
        // should NOT be treated as an escape -- only genuine escapes are rejected.
        assertThatThrownBy(() -> controller.download("issues/../../../../root/.ssh/id_rsa"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void download_withLegitimateNestedKey_resolvesInsideBaseDir_andIsNotFoundRatherThanRejected() throws IOException {
        // A well-formed key that stays within the base dir should reach the
        // "file doesn't exist" path, not the access-denied path.
        assertThatThrownBy(() -> controller.download("issues/" + java.util.UUID.randomUUID() + "/some-file.png"))
                .isInstanceOf(com.nexuspms.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void download_ofExistingFile_returnsIt() throws IOException {
        Path nested = tempDir.resolve("issues/abc");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("file.txt"), "hello");

        var response = controller.download("issues/abc/file.txt");

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
