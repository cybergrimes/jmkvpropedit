package ru.anseranser.jmkvpropedit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for track (video/audio/subtitle) arguments. All three track
 * types share one code path (only the selector differs).
 */
class TrackCommandBuilderTest {

    private static final List<String> FILES = List.of("first.mkv", "second.mkv");

    @Test
    void trackSelectorsAreVideoAudioAndSubtitle() {
        for (char selector : new char[] { 'v', 'a', 's' }) {
            CommandBuilder.Section section = new CommandBuilder().buildTracks(FILES, selector, List.of(named("x")));

            assertEquals(List.of("--edit", "track:" + selector + "1", "--set", "name=x"), section.forFile(0),
                    "selector=" + selector);
        }
    }

    @Test
    void trackNumbersFollowTheSlotIndex() {
        CommandBuilder.Section section = build(named("a"), named("b"), named("c"));

        List<String> expected = List.of(
                "--edit", "track:v1", "--set", "name=a",
                "--edit", "track:v2", "--set", "name=b",
                "--edit", "track:v3", "--set", "name=c");
        assertEquals(expected, section.forFile(0));
        assertEquals(expected, section.forFile(1));
    }

    @Test
    void namesWithQuotesAndBackslashesAreKeptVerbatim() {
        CommandBuilder.Section section = build(named("A\"B\\c"));

        assertEquals(List.of("--edit", "track:v1", "--set", "name=A\"B\\c"), section.forFile(0));
    }

    @Test
    void numberingSubstitutesNumPerFileWithPadding() {
        CommandBuilder.Section section = build(numbered("E{num}", "7", "2"));

        assertEquals(List.of("--edit", "track:v1", "--set", "name=E07"), section.forFile(0));
        assertEquals(List.of("--edit", "track:v1", "--set", "name=E08"), section.forFile(1));
    }

    /**
     * Regression (original JMkvpropedit): with numbering on, the executed
     * variant was built from the display variant, so a name containing quotes
     * produced an invalid options.json.
     */
    @Test
    void numberingKeepsQuotesInTheRealArguments() {
        CommandBuilder.Section section = build(numbered("Commentary \"Director\" {num}", "1", "0"));

        assertEquals(List.of("--edit", "track:v1", "--set", "name=Commentary \"Director\" 1"), section.forFile(0));
        assertEquals(List.of("--edit", "track:v1", "--set", "name=Commentary \"Director\" 2"), section.forFile(1));
    }

    @Test
    void numPlaceholderStaysLiteralWhenNumberingIsOff() {
        CommandBuilder.Section section = build(named("E{num}"));

        assertEquals(List.of("--edit", "track:v1", "--set", "name=E{num}"), section.forFile(1));
    }

    /**
     * Regression: "Edit this track" ticked with nothing set used to emit a bare
     * {@code --edit track:v2} whenever an earlier track had edits, which fails
     * on files without that track.
     */
    @Test
    void editedTrackWithoutPropertiesIsDroppedAfterAPropertyTrack() {
        CommandBuilder.Section section = build(named("x"), editOnly());

        assertEquals(List.of("--edit", "track:v1", "--set", "name=x"), section.forFile(0));
    }

    @Test
    void editedTrackWithoutPropertiesIsDroppedBeforeAPropertyTrack() {
        CommandBuilder.Section section = build(editOnly(), named("x"));

        assertEquals(List.of("--edit", "track:v2", "--set", "name=x"), section.forFile(0));
    }

    @Test
    void uneditedTrackIsSkippedEvenWithSettings() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(false,
                Map.of(),
                true, "x",
                true, "eng",
                true, "--extra",
                true, "1", "1");

        CommandBuilder.Section section = build(track);

        assertEquals(List.of(), section.forFile(0));
        assertEquals(List.of(), section.forFile(1));
    }

    @Test
    void flagsMapToEnabledDefaultForcedBits() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(true,
                Map.of(TrackFlag.FORCED, true, TrackFlag.ENABLED, true, TrackFlag.DEFAULT, false),
                false, "",
                false, null,
                false, "",
                false, "1", "1");

        assertEquals(List.of("--edit", "track:v1", "--set", "flag-enabled=1", "--set", "flag-default=0",
                "--set", "flag-forced=1"), build(track).forFile(0));
    }

    @Test
    void languageIsSetAsOneArgument() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(true,
                Map.of(),
                false, "",
                true, "eng",
                false, "",
                false, "1", "1");

        assertEquals(List.of("--edit", "track:v1", "--set", "language=eng"), build(track).forFile(0));
    }

    @Test
    void extraParametersAreSplitWithBackslashesKept() {
        CommandBuilder.Section section = build(extra("--foo C:\\bar --set \"name=A B\""));

        assertEquals(List.of("--edit", "track:v1", "--foo", "C:\\bar", "--set", "name=A B"), section.forFile(0));
    }

    /** Regression: the old tokenizer treated any apostrophe as a quote and dropped it. */
    @Test
    void extraParametersKeepApostrophes() {
        CommandBuilder.Section section = build(extra("--set name=Director's"));

        assertEquals(List.of("--edit", "track:v1", "--set", "name=Director's"), section.forFile(0));
    }

    @Test
    void extraParametersWithAnUnclosedQuoteAreAnError() {
        assertThrows(ArgumentSplitter.InvalidParametersException.class, () -> build(extra("--set \"name=Oops")));
    }

    @Test
    void blankExtraIsIgnoredAndLeavesNothingToEdit() {
        CommandBuilder.Section section = build(extra("   "));

        assertEquals(List.of(), section.forFile(0));
    }

    @Test
    void fileNamePlaceholderSubstitutesPerFile() {
        CommandBuilder.Section section = build(named("E{file_name}"));

        assertEquals(List.of("--edit", "track:v1", "--set", "name=Efirst"), section.forFile(0));
        assertEquals(List.of("--edit", "track:v1", "--set", "name=Esecond"), section.forFile(1));
    }

    @Test
    void numberingFieldsOfAnUneditedTrackAreNotParsed() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(false,
                Map.of(),
                true, "x",
                false, null,
                false, "",
                true, "bad", "bad");

        assertEquals(List.of(), build(track).forFile(0));
    }

    @Test
    void emptyFileListYieldsNoArguments() {
        CommandBuilder.Section section = new CommandBuilder().buildTracks(List.of(), 'v', List.of(named("x")));

        assertEquals(0, section.size());
    }

    @Test
    void newerFlagsUseMkvpropeditPropertyNamesInDisplayOrder() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(true,
                Map.of(TrackFlag.COMMENTARY, true, TrackFlag.HEARING_IMPAIRED, true,
                        TrackFlag.VISUAL_IMPAIRED, false, TrackFlag.TEXT_DESCRIPTIONS, true,
                        TrackFlag.ORIGINAL, false, TrackFlag.DEFAULT, false),
                false, "",
                false, null,
                false, "",
                false, "1", "1");

        assertEquals(List.of("--edit", "track:v1",
                "--set", "flag-default=0",
                "--set", "flag-original=0",
                "--set", "flag-hearing-impaired=1",
                "--set", "flag-visual-impaired=0",
                "--set", "flag-text-descriptions=1",
                "--set", "flag-commentary=1"), build(track).forFile(0));
    }

    @Test
    void aFlagAloneIsEnoughToEditTheTrack() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(true,
                Map.of(TrackFlag.HEARING_IMPAIRED, true),
                false, "", false, null, false, "", false, "1", "1");

        assertEquals(List.of("--edit", "track:v1", "--set", "flag-hearing-impaired=1"), build(track).forFile(0));
    }

    @Test
    void bcp47LanguageTagIsPassedTrimmed() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(true,
                Map.of(),
                false, "",
                true, "  es-419 ",
                false, "",
                false, "1", "1");

        assertEquals(List.of("--edit", "track:v1", "--set", "language=es-419"), build(track).forFile(0));
    }

    @Test
    void malformedLanguageTagIsAnError() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(true,
                Map.of(),
                false, "",
                true, "pt_BR!",
                false, "",
                false, "1", "1");

        assertThrows(InvalidSettingException.class, () -> build(track));
    }

    @Test
    void languageOfAnUneditedTrackIsNotValidated() {
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(false,
                Map.of(), false, "", true, "not a tag", false, "", false, "1", "1");

        assertEquals(List.of(), build(track).forFile(0));
    }

    @Test
    void flagMapIsCopiedAndExposedThroughTheLegacyAccessors() {
        Map<TrackFlag, Boolean> flags = new java.util.HashMap<>(Map.of(TrackFlag.ENABLED, false));
        CommandBuilder.TrackSettings track = new CommandBuilder.TrackSettings(true, flags,
                false, "", false, null, false, "", false, "1", "1");
        flags.put(TrackFlag.FORCED, true);

        assertTrue(track.setEnabled());
        assertFalse(track.enableValue());
        assertFalse(track.setForced(), "later changes to the caller's map must not leak in");
    }

    /* Settings fixtures */

    private static CommandBuilder.Section build(CommandBuilder.TrackSettings... tracks) {
        return new CommandBuilder().buildTracks(FILES, 'v', List.of(tracks));
    }

    /** An edited track with only a name. */
    static CommandBuilder.TrackSettings named(String name) {
        return new CommandBuilder.TrackSettings(true,
                Map.of(),
                true, name,
                false, null,
                false, "",
                false, "1", "1");
    }

    /** An edited, numbered track with only a name. */
    private static CommandBuilder.TrackSettings numbered(String name, String start, String pad) {
        return new CommandBuilder.TrackSettings(true,
                Map.of(),
                true, name,
                false, null,
                false, "",
                true, start, pad);
    }

    /** An edited track with only extra parameters. */
    private static CommandBuilder.TrackSettings extra(String extra) {
        return new CommandBuilder.TrackSettings(true,
                Map.of(),
                false, "",
                false, null,
                true, extra,
                false, "1", "1");
    }

    /** An edited track without any property. */
    private static CommandBuilder.TrackSettings editOnly() {
        return new CommandBuilder.TrackSettings(true,
                Map.of(),
                false, "",
                false, null,
                false, "",
                false, "1", "1");
    }
}
