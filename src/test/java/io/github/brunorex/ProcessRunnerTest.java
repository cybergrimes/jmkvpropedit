package io.github.brunorex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the process runner — issue #14: launching mkvpropedit,
 * reading its output and the executable probe used to live inside the
 * {@link JMkvpropedit} god class (executeBatch's SwingWorker body,
 * isExecutableInPath and the StreamGobbler reader thread);
 * {@link ProcessRunner} keeps that process layer isolated and free of Swing,
 * so every behavior here runs against a fake InputStream or a substituted
 * command — no window, no UI.
 */
class ProcessRunnerTest {

    @TempDir
    Path tempDir;

    private static String javaExecutable() {
        String exe = Utils.isWindows() ? "java.exe" : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), exe).getPath();
    }

    @Test
    void fakeInputStreamLinesReachTheOutputConsumerInOrder() {
        byte[] data = "first line\nsecond line\n".getBytes(StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();

        ProcessRunner.forwardOutput(new ByteArrayInputStream(data), out::add);

        assertEquals(List.of("first line\n", "second line\n"), out);
    }

    @Test
    void fakeStreamIsDecodedAsExplicitUtf8() {
        String expected = "привет, мир";
        byte[] data = (expected + "\n").getBytes(StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();

        ProcessRunner.forwardOutput(new ByteArrayInputStream(data), out::add);

        assertEquals(List.of(expected + "\n"), out);
    }

    @Test
    void emptyStreamProducesNoOutputAndReturnsAtEof() {
        List<String> out = new ArrayList<>();

        ProcessRunner.forwardOutput(new ByteArrayInputStream(new byte[0]), out::add);

        assertTrue(out.isEmpty(), "no lines expected, got: " + out);
    }

    @Test
    void runForwardsTheMergedStderrOfARealJvmProcessBeforeReturning() throws Exception {
        List<String> out = new ArrayList<>();

        ProcessRunner.run(List.of(javaExecutable(), "-version"), out::add);

        assertFalse(out.isEmpty(),
                "java -version writes only to stderr; the merged stream must arrive before run() returns");
    }

    @Test
    void probeAcceptsTheJvmExecutableOnDisk() {
        assertTrue(new ProcessRunner(javaExecutable()).isExecutableInPath());
    }

    @Test
    void probeRejectsAMissingExecutable() {
        assertFalse(new ProcessRunner("jmkvpropedit-missing-probe.exe").isExecutableInPath());
    }

    @Test
    void runReturnsTheProcessExitCode() throws Exception {
        assertEquals(0, ProcessRunner.run(List.of(javaExecutable(), "-version"), line -> { }));
        assertEquals(1, ProcessRunner.run(List.of(javaExecutable(), "-cp", tempDir.toString(), "NoSuchMainClass"),
                line -> { }));
    }

    @Test
    void optionsFileRunUsesAUniqueTempFileAndRemovesIt() throws Exception {
        ProcessRunner runner = new ProcessRunner(javaExecutable(), tempDir);
        List<String> out = new ArrayList<>();

        // java reads @argfiles too, so "-version" in the file proves the file was passed.
        int exitCode = runner.runWithOptionsFile("-version\n", out::add);

        assertEquals(0, exitCode);
        assertFalse(out.isEmpty(), "the command must have read the options file and produced output");
        try (Stream<Path> left = Files.list(tempDir)) {
            assertEquals(0, left.count(), "options file must be removed after the run");
        }
    }

    @Test
    void optionsFileRunRemovesTheFileWhenTheExecutableIsMissing() throws Exception {
        ProcessRunner runner = new ProcessRunner("jmkvpropedit-missing-probe.exe", tempDir);

        assertThrows(IOException.class, () -> runner.runWithOptions(List.of("a.mkv"), line -> { }));

        try (Stream<Path> left = Files.list(tempDir)) {
            assertEquals(0, left.count(), "options file must be removed even when the launch fails");
        }
    }

    @Test
    void optionsFileDoesNotDependOnTheWorkingDirectory() throws Exception {
        ProcessRunner runner = new ProcessRunner(javaExecutable());
        Path cwdOptions = Path.of("options.json");
        boolean existedBefore = Files.exists(cwdOptions);

        runner.runWithOptionsFile("-version\n", line -> { });

        assertEquals(existedBefore, Files.exists(cwdOptions), "nothing is written to the working directory");
    }
}
