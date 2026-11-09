package ru.anseranser.jmkvpropedit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Launches mkvpropedit and streams its output. No Swing here: output goes to
 * a {@link Consumer}, and callers decide which thread shows it.
 */
public final class ProcessRunner {

    /** How a single mkvpropedit run ended, from its documented exit codes. */
    public enum Outcome {
        /** Exit code 0. */
        SUCCESS,
        /** Exit code 1: the file was modified, but mkvpropedit printed warnings. */
        WARNINGS,
        /** Exit code 2 (or anything else): an error, the file may be unchanged. */
        FAILED;

        public static Outcome of(int exitCode) {
            return switch (exitCode) {
            case 0 -> SUCCESS;
            case 1 -> WARNINGS;
            default -> FAILED;
            };
        }
    }

    private final String executable;
    private final Path tempDirectory;

    /** Options files go to the system temp directory. */
    public ProcessRunner(String executable) {
        this(executable, null);
    }

    /**
     * @param executable    the mkvpropedit executable
     * @param tempDirectory where to create options files; {@code null} for the
     *                      system temp directory (test seam)
     */
    public ProcessRunner(String executable, Path tempDirectory) {
        this.executable = executable;
        this.tempDirectory = tempDirectory;
    }

    /**
     * Runs a command to completion, forwarding stdout and stderr (merged) line
     * by line.
     *
     * @return the process exit code
     * @throws IOException          if the process cannot be started
     * @throws InterruptedException if this thread is interrupted while waiting;
     *                              the process is then destroyed
     */
    public static int run(List<String> command, Consumer<String> output)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process proc = pb.start();
        Thread reader = new Thread(() -> forwardOutput(proc.getInputStream(), output),
                "process-output-reader");
        reader.setDaemon(true);
        reader.start();

        try {
            int exitCode = proc.waitFor();
            // The reader stops at EOF; joining it guarantees the whole
            // process output reaches the log before the caller moves on.
            reader.join();
            return exitCode;
        } catch (InterruptedException e) {
            // Only used for the executable probe: the batch itself never
            // interrupts a running mkvpropedit (it stops between files).
            proc.destroy();
            throw e;
        }
    }

    /**
     * Runs mkvpropedit with the given arguments, passed through a uniquely
     * named JSON options file ({@code mkvpropedit @file.json}) so that no
     * argument is ever re-parsed by a shell. The file is always deleted.
     *
     * @param args the arguments after the executable: the Matroska file, then the edits
     * @return the mkvpropedit exit code
     */
    public int runWithOptions(List<String> args, Consumer<String> output)
            throws IOException, InterruptedException {
        return runWithOptionsFile(OptionsFile.json(args), output);
    }

    /** Lower-level form of {@link #runWithOptions}: writes {@code content} verbatim. */
    int runWithOptionsFile(String content, Consumer<String> output)
            throws IOException, InterruptedException {
        Path optionsFile = tempDirectory == null
                ? Files.createTempFile("jmkvpropedit-", ".json")
                : Files.createTempFile(tempDirectory, "jmkvpropedit-", ".json");

        try {
            Files.writeString(optionsFile, content, StandardCharsets.UTF_8);
            return run(List.of(executable, "@" + optionsFile.toAbsolutePath()), output);
        } finally {
            Files.deleteIfExists(optionsFile);
        }
    }

    /** True if the executable can be started at all (its output is discarded). */
    public boolean isExecutableInPath() {
        try {
            run(List.of(executable), line -> {
                // discard the probe output
            });
            return true;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static void forwardOutput(InputStream is, Consumer<String> output) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;

            while ((line = br.readLine()) != null) {
                output.accept(line + "\n");
            }
        } catch (IOException e) {
            output.accept(e + "\n");
        }
    }
}
