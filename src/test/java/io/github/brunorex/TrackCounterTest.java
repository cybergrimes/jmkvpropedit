package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;

/** Reading a file's track counts from mkvmerge's JSON, and finding mkvmerge. */
class TrackCounterTest {

    /** Trimmed from real mkvmerge v103 output for a Blu-ray rip. */
    private static final String JSON = """
            {
              "attachments": [],
              "chapters": [ { "num_entries": 16 } ],
              "container": { "properties": { "title": "Movie" }, "recognized": true,
                "supported": true, "type": "Matroska" },
              "tracks": [
                { "codec": "VC-1", "id": 0, "properties": { "language": "eng" }, "type": "video" },
                { "codec": "TrueHD", "id": 1, "properties": { "track_name": "Surround 5.1" }, "type": "audio" },
                { "codec": "AC-3", "id": 2, "properties": {}, "type": "audio" },
                { "codec": "HDMV PGS", "id": 3, "properties": { "track_name": "English SDH" }, "type": "subtitles" }
              ]
            }
            """;

    @Test
    void countsEachTrackType() {
        assertEquals(new CommandBuilder.TrackCounts(1, 2, 1), TrackCounter.parse(JSON));
    }

    /** Real mkvmerge v103 output for a text file named .mkv: exit code 0, but not recognised. */
    @Test
    void aFileMkvmergeDoesNotRecogniseGivesNoCounts() {
        assertNull(TrackCounter.parse("""
                { "container": { "recognized": false, "supported": false }, "errors": [],
                  "file_name": "C:/x/damaged.mkv", "identification_format_version": 20, "warnings": [] }
                """));
    }

    /** Real mkvmerge v103 output for an MKV cut off inside its headers: recognised, no tracks, a warning. */
    @Test
    void aDamagedFileWithNoReadableTracksGivesNoCounts() {
        assertNull(TrackCounter.parse("""
                { "container": { "recognized": true, "supported": true, "type": "Matroska" },
                  "errors": [], "tracks": [],
                  "warnings": [ "kax_reader_c::read_headers_internal(): an unknown exception occurred." ] }
                """));
    }

    /** Real mkvmerge v103 output for an MP4 (also when renamed to .mkv): exit code 0, tracks listed. */
    @Test
    void anMp4GivesNoCountsEvenThoughItsTracksAreListed() {
        assertNull(TrackCounter.parse("""
                { "container": { "recognized": true, "supported": true, "type": "MP4/QuickTime" },
                  "tracks": [ { "codec": "AVC/H.264/MPEG-4p10", "id": 0, "type": "video" },
                              { "codec": "AAC", "id": 1, "type": "audio" } ] }
                """));
    }

    /** Real mkvmerge v103 output for a WebM file: reported as Matroska, so it is counted. */
    @Test
    void aWebmFileIsCounted() {
        assertEquals(new CommandBuilder.TrackCounts(1, 0, 0), TrackCounter.parse("""
                { "container": { "recognized": true, "supported": true, "type": "Matroska" },
                  "tracks": [ { "codec": "VP8", "id": 0, "type": "video" } ] }
                """));
    }

    @Test
    void typeTextInsideATrackNameIsNotCounted() {
        String json = "{ \"container\": { \"type\": \"Matroska\" }, \"tracks\": [ { \"properties\": { \"track_name\": "
                + "\"odd \\\"type\\\": \\\"audio\\\" name\" }, \"type\": \"subtitles\" } ] }";

        assertEquals(new CommandBuilder.TrackCounts(0, 0, 1), TrackCounter.parse(json));
    }

    /**
     * Paths are built with {@link File} so the test holds on Windows and on
     * the Linux CI, where a backslash is not a folder separator (regression:
     * a literal "C:\..." path made this test fail on Linux).
     */
    @Test
    void mkvmergeIsLookedForNextToMkvpropedit() {
        File folder = new File("Program Files", "MKVToolNix");
        assertEquals(new File(folder, "mkvmerge.exe").getPath(),
                TrackCounter.mkvmergeFor(new File(folder, "mkvpropedit.exe").getPath()));
        assertEquals(new File(folder, "mkvmerge").getPath(),
                TrackCounter.mkvmergeFor(new File(folder, "mkvpropedit").getPath()));
        assertEquals("mkvmerge", TrackCounter.mkvmergeFor("mkvpropedit"));
        assertEquals("mkvmerge.exe", TrackCounter.mkvmergeFor(" mkvpropedit.exe "));
    }
}
