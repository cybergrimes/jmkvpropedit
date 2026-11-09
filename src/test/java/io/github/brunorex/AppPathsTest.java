package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the per-user settings location and the legacy migration. */
class AppPathsTest {

    @TempDir
    Path tempDir;

    @Test
    void windowsUsesAppData() {
        Path p = AppPaths.configFile("Windows 11", Map.of("APPDATA", "/appdata"), "/home/u");

        assertEquals(Path.of("/appdata", "JMkvpropedit", "JMkvpropedit.ini"), p);
    }

    @Test
    void windowsWithoutAppDataFallsBackToTheRoamingFolder() {
        Path p = AppPaths.configFile("Windows 10", Map.of(), "/home/u");

        assertEquals(Path.of("/home/u", "AppData", "Roaming", "JMkvpropedit", "JMkvpropedit.ini"), p);
    }

    @Test
    void macUsesApplicationSupport() {
        Path p = AppPaths.configFile("Mac OS X", Map.of(), "/Users/u");

        assertEquals(Path.of("/Users/u", "Library", "Application Support", "JMkvpropedit", "JMkvpropedit.ini"), p);
    }

    @Test
    void linuxUsesXdgConfigHome() {
        // Must be absolute on the OS running the test ("/xdg" is not absolute on Windows).
        Path xdg = tempDir.resolve("xdg").toAbsolutePath();
        Path p = AppPaths.configFile("Linux", Map.of("XDG_CONFIG_HOME", xdg.toString()), "/home/u");

        assertEquals(xdg.resolve("jmkvpropedit").resolve("JMkvpropedit.ini"), p);
    }

    @Test
    void linuxDefaultsToDotConfig() {
        Path p = AppPaths.configFile("Linux", Map.of("XDG_CONFIG_HOME", "relative/ignored"), "/home/u");

        assertEquals(Path.of("/home/u", ".config", "jmkvpropedit", "JMkvpropedit.ini"), p);
    }

    @Test
    void legacyFileIsCopiedOnceIntoANewFolder() throws Exception {
        Path legacy = Files.writeString(tempDir.resolve("JMkvpropedit.ini"), "[General]\nmkvpropedit = x\n");
        Path target = tempDir.resolve("cfg").resolve("app").resolve("JMkvpropedit.ini");

        assertTrue(AppPaths.migrateLegacy(legacy, target));
        assertEquals("[General]\nmkvpropedit = x\n", Files.readString(target, StandardCharsets.UTF_8));
        assertTrue(Files.exists(legacy), "the legacy file is left in place");

        assertFalse(AppPaths.migrateLegacy(legacy, target), "never overwrites existing settings");
    }

    @Test
    void nothingToMigrateWithoutALegacyFile() {
        Path target = tempDir.resolve("cfg").resolve("JMkvpropedit.ini");

        assertFalse(AppPaths.migrateLegacy(tempDir.resolve("missing.ini"), target));
        assertFalse(Files.exists(target));
    }
}
