package io.github.brunorex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

/**
 * Keeps mkvpropedit's progress on one line of the output box.
 *
 * <p>
 * While it reads a whole file (for track statistics) mkvpropedit prints
 * "Progress: 0%", "Progress: 1%", ... separated by carriage returns, which a
 * terminal draws over one another but which arrive here as separate lines.
 * A progress line that follows another progress line with the same label
 * replaces it instead of adding up to 100 lines per file. The label is matched
 * loosely so translated output ("Fortschritt: 5%", "Progression : 5 %") works
 * too; anything else is appended unchanged.
 * </p>
 */
final class ProgressOutput {

    /** A short label, a colon and a percentage, e.g. "Progress: 42%". */
    private static final Pattern PROGRESS = Pattern.compile("([^\\d:\\n]{1,40}):\\s?\\d{1,3}\\s?%\\n?");

    private ProgressOutput() {
    }

    /** The progress label of {@code line}, or null if it is not a progress line. */
    static String label(String line) {
        Matcher m = PROGRESS.matcher(line);
        return m.matches() ? m.group(1) : null;
    }

    /**
     * Appends one line of mkvpropedit output, replacing the last line instead
     * when both are progress lines with the same label. Call on the EDT.
     */
    static void append(JTextArea area, String text) {
        String label = label(text);

        if (label != null) {
            try {
                int lines = area.getLineCount();
                // The text ends with "\n", so the last full line is the one before the empty last line.
                if (lines >= 2) {
                    int start = area.getLineStartOffset(lines - 2);
                    int end = area.getLineEndOffset(lines - 2);
                    String previous = area.getText(start, end - start);

                    if (previous.endsWith("\n") && label.equals(label(previous))) {
                        area.replaceRange(text, start, end);
                        return;
                    }
                }
            } catch (BadLocationException e) {
                // Fall through and append; the line count and offsets came from the same document.
            }
        }

        area.append(text);
    }
}
