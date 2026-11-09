# JMkvpropedit

A batch GUI for [mkvpropedit](https://mkvtoolnix.download/docs/mkvpropedit.html) (part of [MKVToolNix](https://mkvtoolnix.download/)), written in Java (Swing).

Apply the same metadata edits — titles, track flags, languages, chapters, tags, attachments — to many Matroska files at once, without typing long command lines by hand.

- **Version:** 1.5.2 (single source of truth: [`gradle.properties`](gradle.properties))
- **Toolchain:** Java 21, Gradle 9
- **License:** BSD-style, see [license.txt](license.txt) (© 2012–2021 Bruno Barbieri)
- **Upstream:** https://github.com/BrunoReX/jmkvpropedit

---

## Features

The main window is a tabbed interface. You configure the edits once, then run them over a whole file list.

### Input

Build the batch file list:

- **Add files** — pick individual Matroska files (`.mkv`, `.mka`, `.mk3d`, `.webm`, `.mks`; case-insensitive).
- **Add folder** — recursive scan; only Matroska extensions are picked up.
- **Reorder / remove / clear** — move selection to top, up, down, bottom; remove or clear the list.
- Drag-and-drop of files onto the text fields where supported (skipped automatically in headless/test environments).

### General

Edits that apply to every file in the batch:

| Setting | What it does |
|--------|----------------|
| **Title** | Set the segment title; supports `{num}` (with start/padding numbering) and `{file_name}` placeholders. |
| **Numbering** | When enabled, `{num}` expands per file (`Start` + `Padding`). |
| **Chapters** | Load chapters from a file (XML/OGM-style sources via the chapter format selector). |
| **Tags** | Load tags from a file (XML). |
| **Extra parameters** | Free-form extra `mkvpropedit` arguments for the general section. |

### Video / Audio / Subtitles

One shared track panel per media type (unified component; the three tabs differ only in labels and the mkvpropedit selector `v` / `a` / `s`):

- **Track list** — combo of tracks for the current type; **Add** / **Remove** tracks (remove is disabled while only one track remains; Opt-mode gets the same `{num}` numbering as the plain command).
- Per-track settings (each with an **Edit this track** switch):
  - Flags, each set to Yes or No only when ticked:
    Enabled, Default, Forced, Original language, Hearing impaired (e.g. SDH subtitles),
    Visual impaired (e.g. audio description), Text descriptions, Commentary.
    The last five need MKVToolNix 57 or newer.
  - Track name (with numbering placeholders)
  - Language: pick from the bundled ISO 639 list, or type an IETF BCP 47 tag
    (`en-US`, `pt-BR`, `es-419`, `zh-Hant`) which then overrides the list. Tags are
    checked for well-formedness before running; mkvpropedit stores both the
    BCP 47 tag and the matching legacy code.
  - Extra parameters
- A track is only edited when **Edit this track** is on *and* at least one property is set, so an empty slot never produces a bare `--edit track:a2` (which fails on files without that track).

#### Extra parameters (General and per track)

Free-form `mkvpropedit` arguments, split like a shell would, with a few friendlier rules:

- Whitespace separates arguments; `"double quotes"` group text anywhere (`--set "title=A B"` or `--set title="A B"`).
- Backslashes are literal (Windows paths need no escaping); inside double quotes `\"` is a literal quote.
- `'single quotes'` group only at the start of an argument, so `name=Director's` keeps its apostrophe.
- An unclosed quote is reported as an error instead of being silently dropped.

### Attachments

Three operations in one panel (unified table + controls):

| Sub-tab | Purpose |
|---------|---------|
| **Add Attachments** | Attach a file with Name / Description / MIME type (MIME list is bundled; `_` placeholder entry is filtered out). |
| **Replace Attachments** | Match an existing attachment by name, ID, or MIME type and replace its data / metadata. |
| **Delete Attachments** | Remove an attachment matched by type + value. |

### Options

- Path to the **`mkvpropedit` executable** (or **Use default** — resolves `mkvpropedit` from `PATH`).
- Persisted in `JMkvpropedit.ini` in the per-user config folder (created on first save):
  - Windows: `%APPDATA%\JMkvpropedit\`
  - macOS: `~/Library/Application Support/JMkvpropedit/`
  - Linux: `$XDG_CONFIG_HOME/jmkvpropedit/` (default `~/.config/jmkvpropedit/`)
- A `JMkvpropedit.ini` left in the working directory by an older version is copied over once on first start.

### Output / run

| Button | Effect |
|--------|--------|
| **Generate command line** | Preview the `mkvpropedit` command(s) that would run. The preview is rendered from the exact argument list that runs, shell-quoted for the current platform, so it can be copy-pasted. |
| **Process files** | Run the batch: for each file, write the arguments to a unique temporary JSON options file, invoke `mkvpropedit @file`, stream UTF-8 output into the **Output** tab, then delete the file. The UI stays responsive (SwingWorker + EDT-safe logging). |
| **Stop after current file** | While a batch runs, the Process button becomes a stop button. mkvpropedit edits files in place, so it is never killed mid-file; closing the window during a batch works the same way. |

The Output tab shows the live log of every invocation, each non-zero `mkvpropedit` exit code, and a closing summary: how many files succeeded, had warnings (exit code 1) or failed (exit code 2), how many were skipped after a stop, and which files need attention.

Folder scans run in the background and add files in sorted (case-insensitive) order, so `{num}` numbering follows episode order.

---

## Requirements

| To… | You need |
|-----|----------|
| **Run the Windows installer / app-image** | Nothing else — Java 21 runtime is bundled. |
| **Run from a jar / distZip** | JDK/JRE **21+** on `PATH` (or via `JAVA_HOME`). |
| **Build from source** | **JDK 21** (Gradle wrapper downloads the rest). |
| **Build `.exe` / `.msi` installer** | JDK 21 + **[WiX Toolset 3](https://wixtoolset.org/)** on `PATH` (Windows only). |
| **Actually edit MKV files** | [MKVToolNix](https://mkvtoolnix.download/) (`mkvpropedit`) installed, path set in **Options**. |

---

## Building

### 1. Compile + test (CI equivalent)

```bash
./gradlew build          # Linux/macOS
.\gradlew.bat build      # Windows
```

- Compiles main + tests, runs the JUnit 5 suite.
- Green build is what GitHub Actions enforces on every push/PR (`.github/workflows/ci.yml`).

### 2. Runnable jar + distribution (no bundled JRE)

```bash
.\gradlew.bat installDist
```

Produces a self-contained app layout:

```
build/install/jmkvpropedit/
├── bin/
│   ├── jmkvpropedit        # shell script
│   └── jmkvpropedit.bat    # Windows script
├── lib/
│   ├── jmkvpropedit-1.5.2.jar
│   └── ini4j-0.5.4.jar
└── ...
```

Run it:

```bash
build\install\jmkvpropedit\bin\jmkvpropedit.bat
```

Plain jars also land in `build/libs/jmkvpropedit-1.5.2.jar` (classes only — you still need `ini4j` on the classpath if you launch with bare `java -jar`; prefer `installDist` or the scripts above).

Zip/tar archives of the same layout:

```bash
.\gradlew.bat distZip      # → build/distributions/jmkvpropedit-1.5.2.zip
```

### 3. Self-contained Windows app / installer (`jpackage`)

Uses the JDK 21 `jpackage` tool. The task is **opt-in** — it is not part of `build`, so plain CI never needs WiX.

```bash
# Portable app image: jmkvpropedit.exe + bundled Java 21 runtime (no WiX required)
.\gradlew.bat jpackage -PjpackageType=app-image

# Windows .exe installer (requires WiX Toolset 3)
.\gradlew.bat jpackage

# Windows .msi installer (requires WiX Toolset 3)
.\gradlew.bat jpackage -PjpackageType=msi
```

Output directory: **`build/jpackage/`**.

| Type | Typical output | Needs WiX | Notes |
|------|----------------|-----------|--------|
| `app-image` | `build/jpackage/jmkvpropedit/jmkvpropedit.exe` + `runtime/` | No | Unpack-and-run; ~150+ MB because of the bundled JRE. |
| `exe` (default) | `build/jpackage/jmkvpropedit-1.5.2.exe` | **Yes** | Installer with Start Menu shortcut and directory chooser. |
| `msi` | `build/jpackage/jmkvpropedit-1.5.2.msi` | **Yes** | Same as exe, MSI package. |

The installer/app-image **bundles a Java 21 runtime**, so end users do not need Java installed.

> **Note:** `jpackage` refuses to overwrite an existing image — the Gradle task cleans `build/jpackage/` before each run.

---

## Project layout

```
├── build.gradle.kts          # application plugin, JUnit 5, jpackage task
├── settings.gradle.kts       # pluginManagement + dependencyResolutionManagement
├── gradle.properties         # version=1.5.2, configuration cache
├── .github/workflows/ci.yml  # gradlew build on push/PR (Ubuntu, Temurin 21)
├── src/
│   ├── main/java/.../        # application sources
│   ├── main/resources/       # icons, langcodes.txt, langnames.txt, mimetypes.txt
│   ├── test/java/.../        # JUnit 5 unit tests
│   └── testHarness/java/.../ # framework-free manual check programs (outside Gradle test task)
└── readme.txt                # short plain-text blurb
```

### Main modules (post-rework)

| Class | Responsibility |
|-------|----------------|
| `JMkvpropedit` | UI shell: window, tabs, wiring only. |
| `CommandBuilder` | Pure settings → `mkvpropedit` argument lists (one value per argument, never re-parsed). |
| `ArgumentSplitter` | Splits the "Extra parameters" fields into arguments. |
| `CommandLineFormatter` | Renders an argument list as a copy-pasteable display line. |
| `OptionsFile` | Writes the `mkvpropedit @file.json` options file. |
| `BatchSummary` | Per-file outcome tally and the end-of-batch report. |
| `AppPaths` | Per-user settings location and one-time migration of an old `JMkvpropedit.ini`. |
| `TrackPanel` / `TrackSlot` / `TrackType` | Unified video/audio/subtitle track UI. |
| `TrackFlag` | The yes/no track flags and their mkvpropedit property names. |
| `LanguageTags` | BCP 47 well-formedness check for typed language tags. |
| `AttachmentPanel` (+ model/operation/selector) | Unified add/replace/delete attachment UI. |
| `ProcessRunner` | Spawn `mkvpropedit`, stream UTF-8 output, temp options-file lifecycle, exit codes. |
| `IniStore` | Read/write `JMkvpropedit.ini` (ini4j). |
| `FileScanner` | Recursive, sorted Matroska folder scan (`Files.walk` + glob). |
| `FileDrop` | Optional drag-and-drop onto text fields (no-op when headless). |
| `Utils` | Context menus and small Swing helpers. |

---

## Tests

```bash
.\gradlew.bat test          # JUnit 5 (src/test)
.\gradlew.bat build         # compile + test + assemble
```

Manual / structural check programs live under `src/testHarness/` and are run individually (they are not wired into the `test` task). They cover track management, file filters, options JSON, Swing threading, repo hygiene, and packaging invariants.

---

## Typical workflow

1. Start the app (installer, app-image, or `installDist` script).
2. **Options** → set the `mkvpropedit` executable (or enable *Use default*).
3. **Input** → add files or a folder.
4. **General / Video / Audio / Subtitles / Attachments** → configure the edits.
5. **Generate command line** → sanity-check the command.
6. **Process files** → run the batch; watch **Output**.

---

## CI

Every push and pull request runs `./gradlew build` on Ubuntu with Temurin 21 (see [`.github/workflows/ci.yml`](.github/workflows/ci.yml)). The workflow also sets the executable bit on `gradlew` so the Linux runner can invoke it.

---

## License

BSD 2-Clause style — see [license.txt](license.txt).
