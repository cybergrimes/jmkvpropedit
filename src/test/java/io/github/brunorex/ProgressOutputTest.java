package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JTextArea;

import org.junit.jupiter.api.Test;

/** Progress percentages from mkvpropedit update one line of the output box. */
class ProgressOutputTest {

    private static String feed(String... lines) {
        JTextArea area = new JTextArea();
        for (String line : lines) {
            ProgressOutput.append(area, line);
        }
        return area.getText();
    }

    @Test
    void progressLinesReplaceEachOther() {
        assertEquals("The file is read in order to create track statistics.\nProgress: 100%\nDone.\n",
                feed("The file is read in order to create track statistics.\n",
                        "Progress: 0%\n", "Progress: 1%\n", "Progress: 73%\n", "Progress: 100%\n",
                        "Done.\n"));
    }

    @Test
    void eachFileKeepsItsOwnProgressLine() {
        assertEquals("Progress: 100%\nDone.\nProgress: 100%\nDone.\n",
                feed("Progress: 0%\n", "Progress: 100%\n", "Done.\n",
                        "Progress: 0%\n", "Progress: 100%\n", "Done.\n"));
    }

    @Test
    void ordinaryLinesAreAppendedUnchanged() {
        assertEquals("The file is being analyzed.\nThe changes are written to the file.\nDone.\n",
                feed("The file is being analyzed.\n", "The changes are written to the file.\n", "Done.\n"));
    }

    @Test
    void firstLineCanBeProgress() {
        assertEquals("Progress: 5%\n", feed("Progress: 1%\n", "Progress: 5%\n"));
    }

    @Test
    void translatedProgressIsRecognised() {
        assertEquals("Progression : 50 %\n", feed("Progression : 10 %\n", "Progression : 50 %\n"));
        assertEquals("Fortschritt: 9%\n", feed("Fortschritt: 3%\n", "Fortschritt: 9%\n"));
    }

    @Test
    void differentLabelsAreNotMerged() {
        assertEquals("Progress: 50%\nOther: 10%\n", feed("Progress: 50%\n", "Other: 10%\n"));
    }

    @Test
    void textMentioningPercentagesIsNotProgress() {
        assertNull(ProgressOutput.label("Warning: 3 of 10 tracks: 30% done\n"));
        assertNull(ProgressOutput.label("Error: C:\\path\\file 50%.mkv\n"));
        assertNull(ProgressOutput.label("Done.\n"));
    }
}
