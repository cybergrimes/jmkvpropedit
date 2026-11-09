package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Unit tests for the mkvpropedit JSON options file. */
class OptionsFileTest {

    @Test
    void argumentsBecomeAJsonArray() {
        assertEquals("[\n  \"a.mkv\",\n  \"--edit\",\n  \"info\"\n]\n",
                OptionsFile.json(List.of("a.mkv", "--edit", "info")));
    }

    @Test
    void emptyListIsAnEmptyArray() {
        assertEquals("[\n]\n", OptionsFile.json(List.of()));
    }

    @Test
    void quotesBackslashesAndControlCharactersAreEscaped() {
        assertEquals("\"C:\\\\a \\\"b\\\"\\tc\\u0001\"", OptionsFile.jsonString("C:\\a \"b\"\tc\u0001"));
    }

    @Test
    void apostrophesAndUnicodeAreLeftAlone() {
        assertEquals("\"it's Привет\"", OptionsFile.jsonString("it's Привет"));
    }

    @Test
    void emptyArgumentIsAnEmptyString() {
        assertEquals("\"\"", OptionsFile.jsonString(""));
    }
}
