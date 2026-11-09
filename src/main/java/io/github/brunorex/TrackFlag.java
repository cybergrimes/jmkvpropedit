package io.github.brunorex;

/**
 * The yes/no track header flags mkvpropedit can set, in the order they are
 * shown and emitted. The property names are mkvpropedit's own ({@code
 * --set flag-hearing-impaired=1}); the five accessibility/commentary/original
 * flags need MKVToolNix 57 or newer.
 */
public enum TrackFlag {
    ENABLED("flag-enabled", "Enabled:", "Whether the track is usable at all"),
    DEFAULT("flag-default", "Default:", "Players pick this track when nothing else is preferred"),
    FORCED("flag-forced", "Forced:", "Must be shown, e.g. subtitles for foreign-language dialogue"),
    ORIGINAL("flag-original", "Original language:",
            "The track is in the content's original language"),
    HEARING_IMPAIRED("flag-hearing-impaired", "Hearing impaired:",
            "For hearing-impaired viewers, e.g. SDH subtitles"),
    VISUAL_IMPAIRED("flag-visual-impaired", "Visual impaired:",
            "For visually impaired viewers, e.g. audio description"),
    TEXT_DESCRIPTIONS("flag-text-descriptions", "Text descriptions:",
            "Contains textual descriptions of video content"),
    COMMENTARY("flag-commentary", "Commentary:", "Contains commentary");

    private final String property;
    private final String label;
    private final String tooltip;

    TrackFlag(String property, String label, String tooltip) {
        this.property = property;
        this.label = label;
        this.tooltip = tooltip;
    }

    /** The mkvpropedit property name, e.g. {@code flag-commentary}. */
    public String property() {
        return property;
    }

    public String label() {
        return label;
    }

    public String tooltip() {
        return tooltip;
    }
}
