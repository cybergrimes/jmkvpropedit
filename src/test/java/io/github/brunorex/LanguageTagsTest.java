package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for the BCP 47 language tag check. */
class LanguageTagsTest {

    @ParameterizedTest
    @ValueSource(strings = { "en", "eng", "en-US", "pt-BR", "es-419", "zh-Hant", "zh-Hant-TW", "sr-Latn-RS",
            "de-CH-1996", "und", "x-custom" })
    void wellFormedTagsAreAccepted(String tag) {
        assertEquals(tag, LanguageTags.validate(tag));
    }

    @ParameterizedTest
    @ValueSource(strings = { "pt_BR", "english language", "e", "en--US", "toolongsubtag1", "en-US!" })
    void malformedTagsAreRejected(String tag) {
        InvalidSettingException e = assertThrows(InvalidSettingException.class, () -> LanguageTags.validate(tag));
        assertTrue(e.getMessage().contains("BCP 47"), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void blankTagIsRejected(String tag) {
        assertThrows(InvalidSettingException.class, () -> LanguageTags.validate(tag));
    }
}
