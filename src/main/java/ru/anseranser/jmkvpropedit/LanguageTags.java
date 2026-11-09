package ru.anseranser.jmkvpropedit;

import java.util.IllformedLocaleException;
import java.util.Locale;

/**
 * Checks custom track language tags before they reach mkvpropedit.
 *
 * <p>
 * mkvpropedit's {@code --set language=...} accepts both ISO 639 codes
 * ({@code eng}) and IETF BCP 47 tags ({@code en-US}, {@code pt-BR},
 * {@code es-419}, {@code zh-Hant}); for a BCP 47 tag it writes the
 * {@code LanguageIETF} element and the closest legacy code. Well-formedness is
 * checked here with the JDK's BCP 47 parser so typos are reported up front;
 * whether a subtag is actually registered is left to mkvpropedit.
 * </p>
 */
public final class LanguageTags {

    private LanguageTags() {
    }

    /**
     * @return the trimmed tag
     * @throws InvalidSettingException if the tag is blank or not well-formed BCP 47
     */
    public static String validate(String tag) {
        String trimmed = tag == null ? "" : tag.trim();

        if (trimmed.isEmpty()) {
            throw new InvalidSettingException("The language tag is empty.");
        }

        try {
            new Locale.Builder().setLanguageTag(trimmed);
        } catch (IllformedLocaleException e) {
            throw new InvalidSettingException("\"" + trimmed + "\" is not a valid BCP 47 language tag"
                    + " (examples: en-US, pt-BR, es-419, zh-Hant).");
        }

        return trimmed;
    }
}
