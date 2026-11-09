package io.github.brunorex;

import javax.swing.JTextArea;

/**
 * Shows mkvpropedit's output in a text area the way a terminal would, so its
 * progress stays on one line.
 *
 * <p>
 * mkvpropedit redraws "Progress: 42%" in place by ending it with a lone
 * carriage return; {@link ProcessRunner#forwardOutput} passes such a line on
 * ending in {@code \r}. That line is shown normally, and the next line
 * replaces it, instead of adding up to 100 lines per file. This works
 * whatever language mkvpropedit prints in, because nothing depends on the
 * wording.
 * </p>
 *
 * <p>
 * If anything else was written to the area in between (the app's own log
 * lines), the replacement is skipped and the next line is simply appended.
 * Use on the EDT only.
 * </p>
 */
final class ProgressOutput {

    private final JTextArea area;

    /** Start of the line the next append replaces, or -1. */
    private int replaceFrom = -1;

    /** The area's length right after that line was written. */
    private int replaceTo = -1;

    ProgressOutput(JTextArea area) {
        this.area = area;
    }

    /** Appends one line from {@link ProcessRunner#forwardOutput}. */
    void append(String text) {
        if (replaceFrom >= 0 && area.getDocument().getLength() == replaceTo) {
            area.replaceRange("", replaceFrom, replaceTo);
        }
        replaceFrom = -1;

        boolean redrawn = text.endsWith("\r");
        int start = area.getDocument().getLength();
        area.append(redrawn ? text.substring(0, text.length() - 1) + "\n" : text);

        if (redrawn) {
            replaceFrom = start;
            replaceTo = area.getDocument().getLength();
        }
    }
}
