# Building and developing JMkvpropedit

This page is for people who want to build JMkvpropedit from source or change the code. If you just want to use the app, download it from the [Releases](../../releases) page.

- **Version:** set in one place, [`gradle.properties`](gradle.properties)
- **Toolchain:** Java 21, Gradle 9 (Kotlin DSL)
- **Coding conventions:** see [AGENTS.md](AGENTS.md)

---

## Requirements

| To… | You need |
|-----|----------|
| **Build from source** | **JDK 21** (the Gradle wrapper downloads the rest). |
| **Run from `installDist` / `distZip`** | Java **21+** on `PATH` (or via `JAVA_HOME`). |
| **Build the portable app-image** | JDK 21 (includes `jpackage`). |
| **Build `.exe` / `.msi` installers** | JDK 21 + **[WiX Toolset 3](https://wixtoolset.org/)** on `PATH` (Windows only). |
| **Actually edit MKV files** | [MKVToolNix](https://mkvtoolnix.download/) (`mkvpropedit`) installed. |

---

## Building

### 1. Compile and test (what CI runs)

```bash
./gradlew build          # Linux/macOS
.\gradlew.bat build      # Windows
```

Compiles the main and test sources and runs the JUnit 5 suite. GitHub Actions runs the same command on every push and pull request (`.github/workflows/ci.yml`).

### 2. Runnable distribution (no bundled Java)

```bash
.\gradlew.bat installDist
```

Produces:

```
build/install/jmkvpropedit/
├── bin/
│   ├── jmkvpropedit        # shell script
│   └── jmkvpropedit.bat    # Windows script
└── lib/
    ├── jmkvpropedit-<version>.jar
    └── ini4j-0.5.4.jar
```

Run it with `build\install\jmkvpropedit\bin\jmkvpropedit.bat`.

The plain jar in `build/libs/` holds only the app's classes; launching it with bare `java -jar` also needs `ini4j` on the classpath, so prefer the scripts above. `.\gradlew.bat distZip` packs the same layout into `build/distributions/jmkvpropedit-<version>.zip`.

### 3. Self-contained Windows app or installer (`jpackage`)

Uses the JDK 21 `jpackage` tool. The task is opt-in: it is not part of `build`, so CI never needs WiX.

```bash
# Portable app image: jmkvpropedit.exe + bundled Java 21 runtime (no WiX needed)
.\gradlew.bat jpackage -PjpackageType=app-image

# Windows .exe installer (needs WiX Toolset 3)
.\gradlew.bat jpackage

# Windows .msi installer (needs WiX Toolset 3)
.\gradlew.bat jpackage -PjpackageType=msi
```

Only the Java modules the app uses are bundled (`--add-modules` in `build.gradle.kts`, found with `jdeps --print-module-deps`); if new code needs another module, add it there. Output goes to `build/jpackage/`; the task cleans that folder first, because `jpackage` refuses to overwrite an existing image. `license.txt`, `readme.txt` and the `licenses/` folder are copied next to the exe, because the licenses require their text to ship with binary downloads.

| Type | Output | Needs WiX | Notes |
|------|--------|-----------|-------|
| `app-image` | `build/jpackage/jmkvpropedit/jmkvpropedit.exe` + `runtime/` | No | Unzip-and-run; about 75 MB (25 MB zipped), most of it the bundled Java. |
| `exe` (default) | `build/jpackage/jmkvpropedit-<version>.exe` | Yes | Installer with Start menu shortcut and folder chooser. |
| `msi` | `build/jpackage/jmkvpropedit-<version>.msi` | Yes | Same, as an MSI package. |

---

## Tests

```bash
.\gradlew.bat test          # JUnit 5 (src/test)
.\gradlew.bat build         # compile + test + assemble
```

`src/testHarness/` holds plain `main()` check programs that run outside Gradle. They open real windows, so they need a display. They cover track management, file filters, the options file, Swing threading, repository hygiene and packaging. Compile them against `build/classes/java/main`, `build/resources/main` and the jars in `build/install/jmkvpropedit/lib`, then run each class.

---

## Making a release

1. Set the new version in `gradle.properties`, for example `version=1.6.1`.
2. Write the release notes in `docs/release-notes/<version>.md`, in plain English for people who use the app. Technical detail belongs in the commit messages.
3. Commit, then tag and push:

   ```bash
   git tag v1.6.1
   git push origin master v1.6.1
   ```

4. The Release workflow (`.github/workflows/release.yml`) checks that the tag matches the version and that the notes exist, runs the tests, builds the portable app, and creates a **draft** release with `jmkvpropedit-<version>-windows.zip`, its SHA-256 checksum, and the notes as its text.
5. Check the draft on the Releases page and press Publish. Nothing is public until then.

If the workflow stops, the draft isn't created. Fix the problem, then move the tag to the new commit (`git tag -f v1.6.1`, `git push -f origin v1.6.1`); this is safe only while the release is still unpublished.

---

## How it works

For each file, the app builds the mkvpropedit arguments as a list, one value per argument. Nothing is joined into a command string and split again, so quotes, apostrophes, backslashes and spaces reach mkvpropedit exactly as typed. The arguments are written to a temporary JSON options file, mkvpropedit is run as `mkvpropedit @file.json`, and the file is deleted afterwards. The command preview is rendered from the same list that runs.

Only the free-text "Extra parameters" fields are split, with these rules:

- Whitespace separates arguments; `"double quotes"` group text anywhere (`--set "title=A B"` or `--set title="A B"`).
- Backslashes are literal (Windows paths need no escaping); inside double quotes, `\"` is a literal quote.
- `'single quotes'` group only at the start of an argument, so `name=Director's` keeps its apostrophe.
- An unclosed quote is reported as an error.

Batches run in a `SwingWorker`. mkvpropedit edits files in place, so it is never killed mid-file: "Stop after current file" and closing the window both take effect between files. Exit code 0 counts as success, 1 as warnings, anything else as failure.

Folder scans run in the background and return files sorted case-insensitively.

Settings (the mkvpropedit path and the picker start folder) are stored in `JMkvpropedit.ini` in the per-user config folder: `%APPDATA%\JMkvpropedit\` on Windows, `~/Library/Application Support/JMkvpropedit/` on macOS, and `$XDG_CONFIG_HOME/jmkvpropedit/` (default `~/.config/jmkvpropedit/`) elsewhere. A `JMkvpropedit.ini` left in the working directory by an older version is copied over once.

---

## Project layout

```
├── build.gradle.kts          # application plugin, JUnit 5, jpackage task
├── settings.gradle.kts
├── gradle.properties         # app version
├── license.txt               # the app's BSD license
├── licenses/                 # third-party licenses shipped with the Windows app
├── .github/workflows/ci.yml  # wrapper check + gradlew build on push/PR (Ubuntu, Temurin 21)
├── .github/workflows/release.yml  # on a v* tag: Windows zip + draft release
├── docs/release-notes/       # plain-English release notes, one file per version
└── src/
    ├── main/java/.../        # application sources
    ├── main/resources/       # icons, language and MIME type lists
    ├── test/java/.../        # JUnit 5 unit tests
    └── testHarness/java/.../ # check programs outside the Gradle test task
```

### Main classes

| Class | Responsibility |
|-------|----------------|
| `JMkvpropedit` | The window: tabs and wiring only. |
| `CommandBuilder` | Settings → mkvpropedit argument lists. No Swing. |
| `ArgumentSplitter` | Splits the "Extra parameters" fields. |
| `CommandLineFormatter` | Renders an argument list as a copy-pasteable command line. |
| `OptionsFile` | Writes the JSON options file. |
| `ProcessRunner` | Runs mkvpropedit, streams UTF-8 output, manages the temp file, returns exit codes. |
| `ProgressOutput` | Keeps mkvpropedit's progress percentages on one line of the output box. |
| `BatchSummary` | Tallies outcomes and writes the end-of-batch report. |
| `AppPaths` | Per-user settings location and migration of an old ini file. |
| `IniStore` | Reads and writes `JMkvpropedit.ini` (ini4j). |
| `StartFolder` | Picks the folder the file pickers open in, falling back to the home folder. |
| `FileScanner` | Recursive, sorted Matroska folder scan. |
| `TrackPanel` / `TrackSlot` / `TrackType` | The shared video/audio/subtitle track UI. |
| `TrackFlag` | The yes/no track flags and their mkvpropedit names. |
| `LanguageTags` | Well-formedness check for typed BCP 47 tags. |
| `AttachmentPanel` (+ model/operation/selector) | The add/replace/delete attachment UI. |
| `FileDrop` | Drag-and-drop onto text fields (no-op when headless). |
| `Utils` | Context menus and small Swing helpers. |
