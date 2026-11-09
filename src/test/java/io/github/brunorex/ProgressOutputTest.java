package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JTextArea;

import org.junit.jupiter.api.Test;

/** mkvpropedit's progress, redrawn with carriage returns, stays on one line of the output box. */
class ProgressOutputTest {

    /** Feeds raw process output through the real reader into a text area. */
    private static String show(String raw) {
        JTextArea area = new JTextArea();
        ProgressOutput out = new ProgressOutput(area);
        ProcessRunner.forwardOutput(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)), out::append);
        return area.getText();
    }

    /** The exact byte layout mkvpropedit v103 writes on Windows. */
    @Test
    void mkvpropeditProgressEndsUpAsOneLine() {
        assertEquals("The file is read in order to create track statistics.\nProgress: 100%\n"
                + "The changes are written to the file.\nDone.\n",
                show("The file is read in order to create track statistics.\r\n"
                        + "Progress: 0%\rProgress: 1%\rProgress: 73%\rProgress: 100%\r\n"
                        + "The changes are written to the file.\r\nDone.\r\n"));
    }

    @Test
    void anyLanguageWorksBecauseTheWordingDoesNotMatter() {
        assertEquals("进度：100%\n完成。\n", show("进度：5%\r进度：42%\r进度：100%\r\n完成。\r\n"));
        assertEquals("Progression : 100 %\n", show("Progression : 10 %\rProgression : 100 %\r\n"));
    }

    @Test
    void similarLookingMessagesOnSeparateLinesAreAllKept() {
        assertEquals("Warning: 5%\nWarning: 12%\n", show("Warning: 5%\r\nWarning: 12%\r\n"));
    }

    @Test
    void ordinaryOutputIsUnchanged() {
        assertEquals("a\nb\nc\n", show("a\r\nb\nc"));
    }

    @Test
    void outputCutOffMidProgressKeepsTheLastValue() {
        assertEquals("Progress: 40%\n", show("Progress: 10%\rProgress: 40%\r"));
    }

    @Test
    void theAppsOwnLinesAreNeverOverwritten() {
        JTextArea area = new JTextArea();
        ProgressOutput out = new ProgressOutput(area);

        out.append("Progress: 40%\r");
        area.append("Error: mkvpropedit stopped\n"); // written by the app, not mkvpropedit
        out.append("File: next.mkv\n");

        assertEquals("Progress: 40%\nError: mkvpropedit stopped\nFile: next.mkv\n", area.getText());
    }
}
