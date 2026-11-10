package io.github.brunorex;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Counts a file's video, audio and subtitle tracks with {@code mkvmerge -J},
 * which ships in the same MKVToolNix folder as mkvpropedit. Used to leave out
 * edits for tracks a file does not have, since mkvpropedit would otherwise
 * refuse to change that file at all.
 */
final class TrackCounter {

    /**
     * A track's {@code "type"} in mkvmerge's JSON. The look-behind skips the
     * same text inside a JSON string (where its quotes would be escaped), such
     * as a track name.
     */
    private static final Pattern TRACK_TYPE = Pattern.compile(
            "(?<!\\\\)\"type\"\\s*:\\s*\"(video|audio|subtitles)\"");

    private TrackCounter() {
    }

    /**
     * The mkvmerge next to the given mkvpropedit: same folder, same extension
     * ({@code C:\...\mkvpropedit.exe} gives {@code C:\...\mkvmerge.exe}; a bare
     * {@code mkvpropedit} gives {@code mkvmerge}, found on the PATH).
     */
    static String mkvmergeFor(String mkvpropedit) {
        File exe = new File(mkvpropedit.trim());
        String name = exe.getName();
        int dot = name.lastIndexOf('.');
        String mkvmerge = "mkvmerge" + (dot > 0 ? name.substring(dot) : "");
        File parent = exe.getParentFile();
        return parent == null ? mkvmerge : new File(parent, mkvmerge).getPath();
    }

    /** mkvmerge recognised the file as Matroska (WebM is reported as Matroska too). */
    private static final Pattern MATROSKA = Pattern.compile("(?<!\\\\)\"type\"\\s*:\\s*\"Matroska\"");

    /**
     * Counts the track types in mkvmerge's {@code -J} output.
     *
     * @return the counts, or null if they can't be trusted: the file is not
     *         Matroska (mkvmerge still exits with 0 for an MP4, even one
     *         renamed to .mkv, or for a file it doesn't recognise at all), or
     *         it has no tracks at all, which is what a damaged file's output
     *         looks like. Every edit is then sent as set, so mkvpropedit
     *         reports the problem and the file counts as failed.
     */
    static CommandBuilder.TrackCounts parse(String json) {
        if (!MATROSKA.matcher(json).find()) {
            return null;
        }

        int video = 0;
        int audio = 0;
        int subtitle = 0;
        Matcher m = TRACK_TYPE.matcher(json);

        while (m.find()) {
            switch (m.group(1)) {
            case "video" -> video++;
            case "audio" -> audio++;
            default -> subtitle++;
            }
        }

        return video + audio + subtitle == 0 ? null : new CommandBuilder.TrackCounts(video, audio, subtitle);
    }

    /**
     * Runs {@code mkvmerge -J file} and counts its tracks.
     *
     * @return the counts, or null if mkvmerge could not read the file (exit
     *         code 2; 1 only means warnings and the tracks are still listed)
     *         or its answer can't be trusted (see {@link #parse})
     * @throws IOException if mkvmerge cannot be started (not installed there)
     */
    static CommandBuilder.TrackCounts count(String mkvmerge, String file)
            throws IOException, InterruptedException {
        StringBuilder json = new StringBuilder();
        int exitCode = ProcessRunner.run(List.of(mkvmerge, "-J", file), json::append);
        return exitCode <= 1 ? parse(json.toString()) : null;
    }
}
