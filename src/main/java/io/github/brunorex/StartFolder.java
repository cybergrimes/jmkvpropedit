package io.github.brunorex;

import java.io.File;

/**
 * Picks the folder the file and folder pickers open in. No Swing, so the
 * rules are unit-testable.
 */
public final class StartFolder {

    private StartFolder() {
    }

    /**
     * Returns the saved start folder when it is set and still exists, and the
     * fallback (the user's home folder) otherwise, for example when the saved
     * folder was on a USB drive that is unplugged or a network share that is
     * offline.
     *
     * @param saved    the folder from the settings, or null when none is set
     * @param fallback the folder to use when {@code saved} can't be used
     */
    public static File resolve(String saved, File fallback) {
        if (saved == null || saved.isBlank()) {
            return fallback;
        }

        File folder = new File(saved);
        return folder.isDirectory() ? folder : fallback;
    }
}
