package io.github.brunorex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Where the application keeps its settings.
 *
 * <p>
 * The settings file used to live in the current working directory, which
 * breaks when the app is installed somewhere read-only (e.g. Program Files
 * via the jpackage installer) and loses settings whenever the app is started
 * from another folder. It now lives in the per-user config folder:
 * </p>
 * <ul>
 * <li>Windows: {@code %APPDATA%\JMkvpropedit\JMkvpropedit.ini}</li>
 * <li>macOS: {@code ~/Library/Application Support/JMkvpropedit/JMkvpropedit.ini}</li>
 * <li>Linux and others: {@code $XDG_CONFIG_HOME/jmkvpropedit/JMkvpropedit.ini}
 * (default {@code ~/.config})</li>
 * </ul>
 * <p>
 * An existing {@code JMkvpropedit.ini} in the working directory is copied
 * over once, so upgrading users keep their mkvpropedit path.
 * </p>
 */
public final class AppPaths {

    public static final String INI_NAME = "JMkvpropedit.ini";

    private AppPaths() {
    }

    /** The settings file for this user, migrating a legacy one if needed. */
    public static Path prepareConfigFile() {
        Path target = configFile(System.getProperty("os.name"), System.getenv(), System.getProperty("user.home"));
        migrateLegacy(Path.of(INI_NAME), target);
        return target;
    }

    /** Pure path computation (test seam). */
    static Path configFile(String osName, Map<String, String> env, String userHome) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        Path home = Path.of(userHome);

        if (os.startsWith("windows")) {
            String appData = env.get("APPDATA");
            Path base = appData != null && !appData.isBlank() ? Path.of(appData)
                    : home.resolve("AppData").resolve("Roaming");
            return base.resolve("JMkvpropedit").resolve(INI_NAME);
        }

        if (os.startsWith("mac")) {
            return home.resolve("Library").resolve("Application Support").resolve("JMkvpropedit").resolve(INI_NAME);
        }

        String xdg = env.get("XDG_CONFIG_HOME");
        Path base = xdg != null && !xdg.isBlank() && Path.of(xdg).isAbsolute() ? Path.of(xdg)
                : home.resolve(".config");
        return base.resolve("jmkvpropedit").resolve(INI_NAME);
    }

    /**
     * Copies {@code legacy} to {@code target} when only the legacy file exists.
     * Best effort: a failure leaves the target missing and the app falls back
     * to its first-run defaults.
     *
     * @return true if a copy was made
     */
    static boolean migrateLegacy(Path legacy, Path target) {
        if (Files.exists(target) || !Files.isRegularFile(legacy)) {
            return false;
        }

        try {
            if (legacy.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                return false;
            }
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.copy(legacy, target);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
