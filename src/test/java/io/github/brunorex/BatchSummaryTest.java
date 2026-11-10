package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Unit tests for the end-of-batch report and exit-code mapping. */
class BatchSummaryTest {

    @Test
    void exitCodesFollowMkvpropeditsDocumentation() {
        assertEquals(ProcessRunner.Outcome.SUCCESS, ProcessRunner.Outcome.of(0));
        assertEquals(ProcessRunner.Outcome.WARNINGS, ProcessRunner.Outcome.of(1));
        assertEquals(ProcessRunner.Outcome.FAILED, ProcessRunner.Outcome.of(2));
        assertEquals(ProcessRunner.Outcome.FAILED, ProcessRunner.Outcome.of(-1));
    }

    @Test
    void reportCountsAndListsProblemFiles() {
        BatchSummary summary = new BatchSummary(3);
        summary.record("a.mkv", ProcessRunner.Outcome.SUCCESS);
        summary.record("b.mkv", ProcessRunner.Outcome.WARNINGS);
        summary.record("c.mkv", ProcessRunner.Outcome.FAILED);

        String report = summary.report();

        assertTrue(report.contains("1 succeeded, 1 with warnings, 1 failed\n"), report);
        assertTrue(report.contains("Failed:\n  c.mkv\n"), report);
        assertTrue(report.contains("With warnings:\n  b.mkv\n"), report);
        assertFalse(report.contains("not processed"), report);
        assertEquals(List.of("c.mkv"), summary.failed());
    }

    @Test
    void reportListsTracksLeftOutPerFile() {
        BatchSummary summary = new BatchSummary(2);
        summary.record("ep1.mkv", ProcessRunner.Outcome.SUCCESS);
        summary.recordSkippedTracks("ep2.mkv", List.of("Audio track 2", "Subtitle track 3"));
        summary.record("ep2.mkv", ProcessRunner.Outcome.SUCCESS);

        String report = summary.report();

        assertTrue(report.contains("2 succeeded, 0 with warnings, 0 failed\n"), report);
        assertTrue(report.contains("Edits skipped for tracks the file doesn't have:\n"
                + "  ep2.mkv (Audio track 2, Subtitle track 3)\n"), report);
    }

    @Test
    void reportOmitsTheSkippedTracksListWhenNothingWasSkipped() {
        BatchSummary summary = new BatchSummary(1);
        summary.record("a.mkv", ProcessRunner.Outcome.SUCCESS);

        assertFalse(summary.report().contains("Edits skipped"), summary.report());
    }

    @Test
    void reportMentionsFilesSkippedByAStop() {
        BatchSummary summary = new BatchSummary(5);
        summary.record("a.mkv", ProcessRunner.Outcome.SUCCESS);
        summary.record("b.mkv", ProcessRunner.Outcome.SUCCESS);

        String report = summary.report();

        assertTrue(report.contains("2 succeeded, 0 with warnings, 0 failed, 3 not processed (stopped)"), report);
        assertFalse(report.contains("Failed:"), report);
    }
}
