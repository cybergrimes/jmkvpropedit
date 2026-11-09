package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StartFolderTest {

    @TempDir
    Path tempDir;

    private File home() {
        return tempDir.resolve("home").toFile();
    }

    @Test
    void anExistingSavedFolderIsUsed() throws Exception {
        Path movies = Files.createDirectory(tempDir.resolve("Movies"));

        assertEquals(movies.toFile(), StartFolder.resolve(movies.toString(), home()));
    }

    @Test
    void noSavedFolderFallsBackToHome() {
        assertEquals(home(), StartFolder.resolve(null, home()));
        assertEquals(home(), StartFolder.resolve("", home()));
        assertEquals(home(), StartFolder.resolve("  ", home()));
    }

    /** An unplugged USB drive or offline network share: open home quietly instead. */
    @Test
    void aMissingSavedFolderFallsBackToHome() {
        String gone = tempDir.resolve("unplugged-drive").resolve("Movies").toString();

        assertEquals(home(), StartFolder.resolve(gone, home()));
    }

    @Test
    void aSavedPathThatIsAFileFallsBackToHome() throws Exception {
        Path file = Files.createFile(tempDir.resolve("movie.mkv"));

        assertEquals(home(), StartFolder.resolve(file.toString(), home()));
    }
}
