package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for the bundled language and MIME type lists. */
class MkvStringsTest {

    @Test
    void languageNamesAreReadAsUtf8() {
        assertTrue(MkvStrings.readLines("langnames.txt").contains("Aasáx"));
    }

    @Test
    void languageNamesAndCodesLineUp() {
        MkvStrings strings = new MkvStrings();
        assertEquals(strings.getLangNameList().size(), strings.getLangCodeList().size());
    }

    @Test
    void missingResourceNamesTheResource() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> MkvStrings.readLines("no-such-resource.txt"));
        assertEquals("Missing resource: no-such-resource.txt", e.getMessage());
    }
}
