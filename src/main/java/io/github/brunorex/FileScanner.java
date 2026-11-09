package io.github.brunorex;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Recursive folder scanning for Matroska files — issue #15.
 *
 * <p>
 * The {@link JMkvpropedit} god class used to run the Files.walk + glob scan
 * itself (java.nio since the commons-io removal in issue #8) as
 * isMatroskaFile/addMkvFilesFromFolder; this scanner isolates that IO with no
 * Swing dependencies, so the UI only forwards the found paths into the file
 * list.
 * </p>
 */
public final class FileScanner {

    /**
     * Folder-scan glob for Matroska files: mkv/mka/mk3d/webm/mks. The glob
     * itself is lowercase; {@link #isMatroskaFile} lowercases the file name
     * before matching so the mask stays case-insensitive on case-sensitive
     * filesystems (Linux/CI) as well as on Windows.
     */
    private static final PathMatcher MATROSKA_FILE_FILTER =
            FileSystems.getDefault().getPathMatcher("glob:*.{mkv,mka,mk3d,webm,mks}");

    private FileScanner() {
    }

    /**
     * Folder-scan mask for Matroska files, case-insensitive: mkv, mka, mk3d,
     * webm, mks (replaces the commons-io {@code WildcardFileFilter}).
     *
     * @param path file to test; only the file name is matched
     * @return true when the file name ends with a Matroska extension
     */
    public static boolean isMatroskaFile(final Path path) {
        final Path name = path.getFileName();

        if (name == null) {
            return false;
        }

        // Lowercase both sides: the glob is fixed lowercase, and normalizing
        // the name makes *.mkv match *.MKV regardless of the filesystem's
        // own case sensitivity.
        return MATROSKA_FILE_FILTER.matches(
                Path.of(name.toString().toLowerCase(Locale.ROOT)));
    }

    /**
     * Recursively scans a folder for Matroska files, same as the former
     * FileUtils.iterateFiles with TrueFileFilter dir filter: the mask applies
     * to file names only, subfolders are included.
     *
     * @param folder root of the scan
     * @return the matching regular files, in walk order
     * @throws IOException when the folder cannot be walked
     */
    public static List<Path> scanMatroskaFiles(final Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(Files::isRegularFile)
                    .filter(FileScanner::isMatroskaFile)
                    // Disk order is arbitrary; {num} numbering follows list
                    // order, so sort like a file manager would (case-insensitive).
                    .sorted(Comparator.comparing((Path p) -> p.toString().toLowerCase(Locale.ROOT))
                            .thenComparing(Comparator.naturalOrder()))
                    .toList();
        }
    }
}
