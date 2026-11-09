package ru.anseranser.jmkvpropedit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Unit tests for the display command line. */
class CommandLineFormatterTest {

    @Test
    void plainArgumentsAreNotQuoted() {
        assertEquals("mkvpropedit a.mkv --edit track:v1 --set flag-default=1",
                CommandLineFormatter.format(List.of("mkvpropedit", "a.mkv", "--edit", "track:v1", "--set",
                        "flag-default=1"), false));
    }

    @Test
    void emptyArgumentIsShownAsEmptyQuotes() {
        assertEquals("''", CommandLineFormatter.quote("", false));
        assertEquals("\"\"", CommandLineFormatter.quote("", true));
    }

    @Test
    void posixUsesSingleQuotesAndEscapesApostrophes() {
        assertEquals("'name=it'\\''s \"here\"'", CommandLineFormatter.quote("name=it's \"here\"", false));
    }

    @Test
    void windowsKeepsPlainPathsUnquoted() {
        assertEquals("C:\\TV\\a.mkv", CommandLineFormatter.quote("C:\\TV\\a.mkv", true));
    }

    @Test
    void windowsQuotesSpacesAndEscapesQuotes() {
        assertEquals("\"title=A \\\"B\\\"\"", CommandLineFormatter.quote("title=A \"B\"", true));
    }

    @Test
    void windowsDoublesBackslashesBeforeAQuoteOrTheClosingQuote() {
        // C runtime rules: "a\\\"b" -> a\"b and a trailing backslash must not escape the closing quote.
        assertEquals("\"a\\\\\\\"b c\"", CommandLineFormatter.quote("a\\\"b c", true));
        assertEquals("\"C:\\my dir\\\\\"", CommandLineFormatter.quote("C:\\my dir\\", true));
    }
}
