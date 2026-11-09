package ru.anseranser.jmkvpropedit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the batch assembly: one entry per file, the canonical section
 * order, real arguments that start with the file, and display lines rendered
 * from the very same arguments.
 */
class CommandBuilderBatchTest {

    @Test
    void displayLineQuotesOnlyWhatNeedsQuotingOnWindows() {
        CommandBuilder.Batch batch = new CommandBuilder(true).buildBatch("C:\\Program Files\\MKVToolNix\\mkvpropedit.exe",
                List.of("D:\\TV\\Show S01E01.mkv"), section("--edit", "info"), List.of(), blank(), blank(), blank());

        assertEquals("\"C:\\Program Files\\MKVToolNix\\mkvpropedit.exe\" \"D:\\TV\\Show S01E01.mkv\" --edit info",
                batch.lines().get(0));
    }

    @Test
    void displayLineUsesSingleQuotesOnPosix() {
        CommandBuilder.Batch batch = new CommandBuilder(false).buildBatch("mkvpropedit",
                List.of("/tv/Bob's Show.mkv"), section("--set", "title=A \"B\""), List.of(), blank(), blank(),
                blank());

        assertEquals("mkvpropedit '/tv/Bob'\\''s Show.mkv' --set 'title=A \"B\"'", batch.lines().get(0));
    }

    @Test
    void argsStartWithTheFileAndExcludeTheExe() {
        CommandBuilder.Batch batch = new CommandBuilder(true).buildBatch("mkvpropedit", List.of("first.mkv"),
                section("--edit", "info"), List.of(), blank(), blank(), blank());

        assertEquals(List.of("first.mkv", "--edit", "info"), batch.args().get(0));
    }

    @Test
    void argsKeepAwkwardFileNamesAndValuesVerbatim() {
        List<String> files = List.of("C:\\a\"b\\it's c.mkv");
        CommandBuilder.Section video = new CommandBuilder(true).buildTracks(files, 'v',
                List.of(TrackCommandBuilderTest.named("x\"y")));

        CommandBuilder.Batch batch = new CommandBuilder(true).buildBatch("mkvpropedit", files,
                blank(), List.of(), video, blank(), blank());

        assertEquals(List.of("C:\\a\"b\\it's c.mkv", "--edit", "track:v1", "--set", "name=x\"y"),
                batch.args().get(0));
    }

    @Test
    void sectionsComposeInCanonicalOrder() {
        CommandBuilder.Batch batch = new CommandBuilder(true).buildBatch("mkvpropedit", List.of("first.mkv"),
                section("G"), List.of("D", "A", "R"), section("V"), section("U"), section("S"));

        // canonical order: general, attachments(delete, add, replace), video, audio, subtitle
        assertEquals("mkvpropedit first.mkv G D A R V U S", batch.lines().get(0));
        assertEquals(List.of("first.mkv", "G", "D", "A", "R", "V", "U", "S"), batch.args().get(0));
    }

    @Test
    void oneEntryPerFile() {
        CommandBuilder.Section general = new CommandBuilder.Section(List.of(List.of("G0"), List.of("G1")));

        CommandBuilder.Batch batch = new CommandBuilder(true).buildBatch("mkvpropedit",
                List.of("first.mkv", "second.mkv"), general, List.of(), blanks(2), blanks(2), blanks(2));

        assertEquals(2, batch.lines().size());
        assertEquals(2, batch.args().size());
        assertEquals("mkvpropedit first.mkv G0", batch.lines().get(0));
        assertEquals("mkvpropedit second.mkv G1", batch.lines().get(1));
    }

    @Test
    void displayLineAndRealArgumentsAgree() {
        // The display line, split back with the same rules a user would expect
        // from "Extra parameters", gives exactly the real arguments.
        List<String> files = List.of("/tv/Show \"Pilot\" - it's here.mkv");
        CommandBuilder builder = new CommandBuilder(false);
        CommandBuilder.Section general = builder.buildGeneral(files, new CommandBuilder.GeneralSettings(
                new CommandBuilder.SourceSetting(false, 0, "", ".xml"),
                new CommandBuilder.SourceSetting(true, 0, "", ".xml"),
                new CommandBuilder.TitleSetting(true, false, "A \"quoted\" title", "1", "0"), false, ""));

        CommandBuilder.Batch batch = builder.buildBatch("mkvpropedit", files, general, List.of(),
                blank(), blank(), blank());

        List<String> expected = new ArrayList<>(List.of("mkvpropedit"));
        expected.addAll(batch.args().get(0));
        assertEquals(expected, PosixShell.split(batch.lines().get(0)));
    }

    @Test
    void nothingToDoProducesAnEmptyBatch() {
        CommandBuilder.Batch batch = new CommandBuilder(true).buildBatch("mkvpropedit", List.of("first.mkv"),
                blank(), List.of(), blank(), blank(), blank());

        assertTrue(batch.lines().isEmpty());
        assertTrue(batch.args().isEmpty());
    }

    @Test
    void emptyFileListProducesAnEmptyBatch() {
        CommandBuilder.Batch batch = new CommandBuilder(true).buildBatch("mkvpropedit", List.of(),
                new CommandBuilder.Section(List.of()), List.of("--delete-attachment", "1"),
                new CommandBuilder.Section(List.of()), new CommandBuilder.Section(List.of()),
                new CommandBuilder.Section(List.of()));

        assertTrue(batch.lines().isEmpty());
        assertTrue(batch.args().isEmpty());
    }

    /* Fixtures */

    private static CommandBuilder.Section section(String... args) {
        return new CommandBuilder.Section(List.of(List.of(args)));
    }

    private static CommandBuilder.Section blank() {
        return blanks(1);
    }

    private static CommandBuilder.Section blanks(int count) {
        List<List<String>> perFile = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            perFile.add(List.of());
        }
        return new CommandBuilder.Section(perFile);
    }

    /** Minimal POSIX-shell word splitting (single quotes, double quotes, backslash). */
    static final class PosixShell {
        static List<String> split(String line) {
            List<String> words = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            boolean inWord = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\'') {
                    inWord = true;
                    int end = line.indexOf('\'', i + 1);
                    cur.append(line, i + 1, end);
                    i = end;
                } else if (c == '"') {
                    inWord = true;
                    for (i++; line.charAt(i) != '"'; i++) {
                        if (line.charAt(i) == '\\' && "\"\\$`".indexOf(line.charAt(i + 1)) >= 0) {
                            i++;
                        }
                        cur.append(line.charAt(i));
                    }
                } else if (c == '\\') {
                    inWord = true;
                    cur.append(line.charAt(++i));
                } else if (c == ' ') {
                    if (inWord) {
                        words.add(cur.toString());
                        cur.setLength(0);
                        inWord = false;
                    }
                } else {
                    inWord = true;
                    cur.append(c);
                }
            }
            if (inWord) {
                words.add(cur.toString());
            }
            return words;
        }
    }
}
