package io.github.brunorex;

import java.util.ArrayList;
import java.util.List;

/**
 * Tallies the outcome of each file in a batch and renders the end-of-run
 * report, so a long batch does not have to be read line by line to find the
 * files that failed. Not thread-safe; used by the single batch worker.
 */
public final class BatchSummary {

    private final int total;
    private int succeeded;
    private final List<String> warned = new ArrayList<>();
    private final List<String> failed = new ArrayList<>();

    public BatchSummary(int total) {
        this.total = total;
    }

    public void record(String file, ProcessRunner.Outcome outcome) {
        switch (outcome) {
        case SUCCESS -> succeeded++;
        case WARNINGS -> warned.add(file);
        case FAILED -> failed.add(file);
        }
    }

    public int processed() {
        return succeeded + warned.size() + failed.size();
    }

    public List<String> failed() {
        return List.copyOf(failed);
    }

    public List<String> warned() {
        return List.copyOf(warned);
    }

    public String report() {
        StringBuilder sb = new StringBuilder("==============\nSummary: ");
        sb.append(succeeded).append(" succeeded, ")
                .append(warned.size()).append(" with warnings, ")
                .append(failed.size()).append(" failed");

        int skipped = total - processed();
        if (skipped > 0) {
            sb.append(", ").append(skipped).append(" not processed (stopped)");
        }
        sb.append('\n');

        appendList(sb, "Failed:", failed);
        appendList(sb, "With warnings:", warned);
        return sb.toString();
    }

    private static void appendList(StringBuilder sb, String heading, List<String> files) {
        if (files.isEmpty()) {
            return;
        }

        sb.append(heading).append('\n');
        for (String file : files) {
            sb.append("  ").append(file).append('\n');
        }
    }
}
