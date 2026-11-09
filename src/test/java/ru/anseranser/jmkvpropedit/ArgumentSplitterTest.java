package ru.anseranser.jmkvpropedit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Unit tests for splitting the "Extra parameters" fields. */
class ArgumentSplitterTest {

    @Test
    void whitespaceSeparatesArguments() {
        assertEquals(List.of("--set", "flag-default=1"), ArgumentSplitter.split("  --set \t flag-default=1  "));
    }

    @Test
    void nullAndBlankGiveNoArguments() {
        assertEquals(List.of(), ArgumentSplitter.split(null));
        assertEquals(List.of(), ArgumentSplitter.split("   "));
    }

    @Test
    void doubleQuotesGroupAtTheStartOfAnArgument() {
        assertEquals(List.of("--set", "title=Hello World"), ArgumentSplitter.split("--set \"title=Hello World\""));
    }

    @Test
    void doubleQuotesGroupInsideAnArgument() {
        assertEquals(List.of("--set", "title=Hello World"), ArgumentSplitter.split("--set title=\"Hello World\""));
    }

    @Test
    void escapedQuoteInsideDoubleQuotesIsLiteral() {
        assertEquals(List.of("name=A \"B\""), ArgumentSplitter.split("\"name=A \\\"B\\\"\""));
    }

    @Test
    void backslashesAreLiteral() {
        assertEquals(List.of("C:\\dir\\file.xml", "C:\\with space\\ch.xml", "\\\\server\\share"),
                ArgumentSplitter.split("C:\\dir\\file.xml \"C:\\with space\\ch.xml\" \"\\\\server\\share\""));
    }

    @Test
    void apostropheInsideAWordIsLiteral() {
        assertEquals(List.of("name=Director's", "Cut"), ArgumentSplitter.split("name=Director's Cut"));
    }

    @Test
    void singleQuotesGroupAtTheStartOfAnArgument() {
        assertEquals(List.of("name=A \"B\""), ArgumentSplitter.split("'name=A \"B\"'"));
    }

    @Test
    void emptyQuotesGiveAnEmptyArgument() {
        assertEquals(List.of("--chapters", ""), ArgumentSplitter.split("--chapters \"\""));
        assertEquals(List.of("--chapters", ""), ArgumentSplitter.split("--chapters ''"));
    }

    @Test
    void unicodeIsKept() {
        assertEquals(List.of("title=Привет 世界"), ArgumentSplitter.split("\"title=Привет 世界\""));
    }

    @Test
    void unclosedDoubleQuoteIsAnError() {
        assertThrows(ArgumentSplitter.InvalidParametersException.class,
                () -> ArgumentSplitter.split("--set \"name=Oops"));
    }

    @Test
    void unclosedSingleQuoteIsAnError() {
        assertThrows(ArgumentSplitter.InvalidParametersException.class,
                () -> ArgumentSplitter.split("'oops"));
    }
}
