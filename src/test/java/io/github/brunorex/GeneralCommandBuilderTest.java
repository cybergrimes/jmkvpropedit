package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the General-tab arguments: tags/chapters modes, title
 * numbering across files, and values kept verbatim as single arguments.
 */
class GeneralCommandBuilderTest {

    private static final List<String> FILES = List.of("first.mkv", "second.mkv");

    @Test
    void tagsRemoveAppendsBareAllSelector() {
        CommandBuilder.Section section = build(general(tags(0, ""), off(), titleOff(), false, ""));

        assertEquals(List.of("--tags", "all:"), section.forFile(0));
        assertEquals(List.of("--tags", "all:"), section.forFile(1));
    }

    @Test
    void tagsFromFileKeepsTheValueVerbatim() {
        CommandBuilder.Section section = build(general(tags(1, "C:\\my tags\\a\"b.xml"), off(), titleOff(), false, ""));

        assertEquals(List.of("--tags", "all:C:\\my tags\\a\"b.xml"), section.forFile(0));
    }

    @Test
    void tagsFromFileWithBlankTextFallsBackToRemove() {
        CommandBuilder.Section section = build(general(tags(1, "   "), off(), titleOff(), false, ""));

        assertEquals(List.of("--tags", "all:"), section.forFile(0));
    }

    @Test
    void tagsMatchSuffixBuildsPathWithoutExtension() {
        CommandBuilder.Section section = new CommandBuilder(true).buildGeneral(List.of("C:\\clips\\first.mkv"),
                general(tags(2, "-meta"), off(), titleOff(), false, ""));

        assertEquals(List.of("--tags", "all:C:\\clips\\first-meta.xml"), section.forFile(0));
    }

    @Test
    void chaptersRemovePassesAnEmptyArgument() {
        CommandBuilder.Section section = build(general(off(), chapters(0, ""), titleOff(), false, ""));

        assertEquals(List.of("--chapters", ""), section.forFile(0));
    }

    @Test
    void chaptersFromFileKeepsTheValueVerbatim() {
        CommandBuilder.Section section = build(general(off(), chapters(1, "c\"d"), titleOff(), false, ""));

        assertEquals(List.of("--chapters", "c\"d"), section.forFile(0));
    }

    @Test
    void titleNumberingIncrementsAcrossFilesAndSubstitutesFileName() {
        CommandBuilder.Section section = build(general(off(), off(),
                new CommandBuilder.TitleSetting(true, true, "Ep {num}: {file_name}", "1", "2"), false, ""));

        assertEquals(List.of("--edit", "info", "--set", "title=Ep 01: first"), section.forFile(0));
        assertEquals(List.of("--edit", "info", "--set", "title=Ep 02: second"), section.forFile(1));
    }

    @Test
    void titleKeepsNumLiteralWhenNumberingIsOff() {
        CommandBuilder.Section section = build(general(off(), off(),
                new CommandBuilder.TitleSetting(true, false, "Ep {num}", "1", "2"), false, ""));

        assertEquals(List.of("--edit", "info", "--set", "title=Ep {num}"), section.forFile(1));
    }

    @Test
    void titleWithQuotesApostrophesAndBackslashesIsOneVerbatimArgument() {
        String title = "He said \"hi\" \\ it's {num}";
        CommandBuilder.Section section = build(general(off(), off(),
                new CommandBuilder.TitleSetting(true, true, title, "1", "0"), false, ""));

        assertEquals(List.of("--edit", "info", "--set", "title=He said \"hi\" \\ it's 1"), section.forFile(0));
    }

    /** Regression: the preview showed one argument, the run received two. */
    @Test
    void generalExtraHonoursDoubleQuotes() {
        CommandBuilder.Section section = build(general(off(), off(), titleOff(), true,
                "--set \"title=Hello World\""));

        assertEquals(List.of("--set", "title=Hello World"), section.forFile(0));
    }

    @Test
    void generalExtraKeepsWindowsPathsAndApostrophes() {
        CommandBuilder.Section section = build(general(off(), off(), titleOff(), true,
                "--chapters C:\\dir\\ch.xml --set title=Director's"));

        assertEquals(List.of("--chapters", "C:\\dir\\ch.xml", "--set", "title=Director's"), section.forFile(0));
    }

    @Test
    void generalExtraWithAnUnclosedQuoteIsAnError() {
        assertThrows(ArgumentSplitter.InvalidParametersException.class,
                () -> build(general(off(), off(), titleOff(), true, "--set \"title=Oops")));
    }

    @Test
    void blankGeneralExtraIsSkipped() {
        CommandBuilder.Section section = build(general(off(), off(), titleOff(), true, "   "));

        assertEquals(List.of(), section.forFile(0));
        assertEquals(List.of(), section.forFile(1));
    }

    @Test
    void disabledExtraIsIgnoredEvenIfInvalid() {
        CommandBuilder.Section section = build(general(off(), off(), titleOff(), false, "\"unclosed"));

        assertEquals(List.of(), section.forFile(0));
    }

    @Test
    void disabledSettingsProduceEmptyArguments() {
        CommandBuilder.Section section = build(general(off(), off(), titleOff(), false, ""));

        assertEquals(2, section.size());
        assertEquals(List.of(), section.forFile(0));
        assertEquals(List.of(), section.forFile(1));
    }

    @Test
    void numberingFieldsAreParsedOnlyWhenNumberingIsUsed() {
        CommandBuilder.Section section = build(general(off(), off(),
                new CommandBuilder.TitleSetting(true, false, "T", "not-a-number", "x"), false, ""));

        assertEquals(List.of("--edit", "info", "--set", "title=T"), section.forFile(0));
    }

    @Test
    void badNumberingFieldIsAnErrorWhenNumberingIsOn() {
        assertThrows(NumberFormatException.class, () -> build(general(off(), off(),
                new CommandBuilder.TitleSetting(true, true, "T {num}", "not-a-number", "1"), false, "")));
    }

    @Test
    void trackStatisticsAddsTheStatisticsOptionForEveryFile() {
        CommandBuilder.Section section = build(new CommandBuilder.GeneralSettings(off(), off(), titleOff(),
                false, "", true));

        assertEquals(List.of("--add-track-statistics-tags"), section.forFile(0));
        assertEquals(List.of("--add-track-statistics-tags"), section.forFile(1));
    }

    @Test
    void trackStatisticsComeAfterTagsAndTitleAndBeforeExtra() {
        CommandBuilder.Section section = build(new CommandBuilder.GeneralSettings(tags(0, ""), off(),
                new CommandBuilder.TitleSetting(true, false, "T", "1", "1"), true, "--quiet", true));

        assertEquals(List.of("--tags", "all:", "--edit", "info", "--set", "title=T",
                "--add-track-statistics-tags", "--quiet"), section.forFile(0));
    }

    @Test
    void trackStatisticsAreOffByDefault() {
        assertEquals(false, general(off(), off(), titleOff(), false, "").trackStatistics());
    }

    /* Settings fixtures */

    private static CommandBuilder.Section build(CommandBuilder.GeneralSettings settings) {
        return new CommandBuilder().buildGeneral(FILES, settings);
    }

    private static CommandBuilder.GeneralSettings general(CommandBuilder.SourceSetting tags,
            CommandBuilder.SourceSetting chapters, CommandBuilder.TitleSetting title,
            boolean extraEnabled, String extra) {
        return new CommandBuilder.GeneralSettings(tags, chapters, title, extraEnabled, extra);
    }

    private static CommandBuilder.SourceSetting tags(int mode, String text) {
        return new CommandBuilder.SourceSetting(true, mode, text, ".xml");
    }

    private static CommandBuilder.SourceSetting chapters(int mode, String text) {
        return new CommandBuilder.SourceSetting(true, mode, text, ".xml");
    }

    private static CommandBuilder.SourceSetting off() {
        return new CommandBuilder.SourceSetting(false, 0, "", ".xml");
    }

    private static CommandBuilder.TitleSetting titleOff() {
        return new CommandBuilder.TitleSetting(false, false, "", "1", "1");
    }
}
