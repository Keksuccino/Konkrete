package de.keksuccino.konkrete.util.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceFileConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @AfterEach
    void resetAssetDirectory() {
        ResourceFile.resetAssetsDirectory();
    }

    @Test
    void configuredAssetDirectoryIsStoredAsAbsoluteFile() {
        Path relativeDirectory = Path.of("").toAbsolutePath().relativize(this.temporaryDirectory.toAbsolutePath());

        ResourceFile.setAssetsDirectory(relativeDirectory.toFile());

        assertEquals(relativeDirectory.toFile().getAbsoluteFile(), ResourceFile.getAssetsDirectory());
    }

    @Test
    void nullAssetDirectoryIsRejected() {
        assertThrows(NullPointerException.class, () -> ResourceFile.setAssetsDirectory(null));
    }
}
