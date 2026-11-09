# AGENTS.md

Notes for anyone (human or coding agent) changing this repository.

## Layout

| Path | Contents |
|---|---|
| `src/main/java/io/github/brunorex/` | The application. `JMkvpropedit` is the Swing window; `CommandBuilder` turns settings into mkvpropedit arguments; `ProcessRunner` runs them. |
| `src/main/resources/` | Language names/codes, MIME types (UTF-8 text, one entry per line) and icons. |
| `src/test/java/` | JUnit 5 unit tests, run by `gradlew build`. |
| `src/testHarness/java/` | Plain `main()` check programs outside Gradle. They open real windows, so they need a display. |
| `.github/workflows/ci.yml` | CI: `./gradlew build` on Ubuntu with JDK 21. |

## Build

- `gradlew build`: compile and run the unit tests (Java 21 toolchain).
- `gradlew installDist`: runnable copy in `build/install/jmkvpropedit/`.
- `gradlew jpackage -PjpackageType=app-image`: self-contained app with a bundled runtime. The default type builds an installer and needs WiX on Windows.

## Conventions

- **Never build a command as a string to be re-parsed.** Add each argument to a `List<String>`. Only the user's free-text "Extra parameters" go through `ArgumentSplitter`.
- The command preview is rendered from the same list that runs (`CommandLineFormatter`), so the two can't drift apart.
- `CommandBuilder` stays Swing-free and unit-tested. UI classes only read widgets into its settings records.
- User-input problems throw `InvalidSettingException` (or `NumberFormatException` for the numbering fields); `JMkvpropedit.setCmdLine()` shows them in a dialog.
- Never kill mkvpropedit mid-file: it edits files in place. Stopping a batch takes effect between files.
- Background work runs in a `SwingWorker`; UI updates happen only on the EDT (`appendOutput` is EDT-safe).
- Settings live in the per-user folder from `AppPaths`, never in the working directory. Temporary options files come from `Files.createTempFile` and are always deleted.
- Tests are named after the behaviour they check, and every bug fix gets a regression test.
