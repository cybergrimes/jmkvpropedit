# JMkvpropedit

JMkvpropedit lets you change the title, track names, languages and flags of many MKV files at once. It's useful for tidying up a TV series or a whole movie library so your media server shows everything correctly. It doesn't remux or re-encode, so even large files are done in seconds.

## Getting started (Windows)

1. Install [MKVToolNix](https://mkvtoolnix.download/). JMkvpropedit uses its mkvpropedit tool to make the changes.
2. Download the latest `jmkvpropedit-…-windows.zip` from [Releases](../../releases), unzip it anywhere, and run `jmkvpropedit.exe`. Java is included, so you don't need to install it. Keep the `.exe` together with the `app` and `runtime` folders next to it.
3. On the Options tab, point the app at `mkvpropedit.exe`, usually `C:\Program Files\MKVToolNix\mkvpropedit.exe`.

If Windows says "Windows protected your PC", click More info, then Run anyway. The warning appears because the app isn't signed.

## Example: mark the second subtitle track of a series as SDH

1. On the Input tab, click Add folder and choose the season folder.
2. On the Subtitles tab, click the + button next to the track list so it shows Subtitle Track 2.
3. Tick Edit this track, then tick Hearing impaired and choose Yes.
4. Click Process files. The Output tab shows each file as it's done, then a summary.

Changes are written straight into your files and can't be undone. Try a new batch on a copy of one or two files first.

## What you can change

You set up the changes once, and they are applied to every file in the list.

**Input.** Add files one by one, or add a whole folder (including subfolders). Files can be reordered or removed. Files added from a folder are listed in name order.

**General.** Settings for the file as a whole:

- Title. Use `{file_name}` to insert the file's name, or `{num}` for a running number (1, 2, 3… or 01, 02, 03… using the Start and Padding boxes).
- Chapters and tags, loaded from a file or removed.

**Video, Audio and Subtitles.** Settings for individual tracks:

- Track 1, track 2 and so on count tracks of that type only, so Audio Track 2 is the second audio track. The same position is used in every file, so this works best when your files are laid out the same way.
- Track name, which can also use `{num}` and `{file_name}`.
- Language. Choose from the list, or type a regional code such as `pt-BR` (Brazilian Portuguese), `es-419` (Latin American Spanish) or `zh-Hant` (Traditional Chinese).
- Flags, each set to Yes or No: Enabled, Default, Forced, Original language, Hearing impaired, Visual impaired, Text descriptions and Commentary. Media servers such as Emby, Jellyfin and Plex use these to label and choose tracks. The last five need MKVToolNix 57 or newer.

Only tracks with Edit this track ticked are changed, and only the settings you tick.

**Attachments.** Add, replace or delete attached files such as fonts or cover images.

**Running a batch.** Generate command line shows exactly what will run, without changing anything. Process files runs the batch. While it runs, the button changes to Stop after current file, which finishes the file in progress and then stops. When the batch ends, the Output tab lists which files worked, which had warnings and which failed.

**Extra parameters.** If you know mkvpropedit's own options, you can type extra ones in the General tab or for each track. Put a value in double quotes if it contains spaces, for example `--set "title=My Film"`.

## Other systems

The release download is for Windows. On macOS and Linux, install Java 21 and MKVToolNix, then build the app from source as described in [BUILDING.md](BUILDING.md).

## Settings

The only setting the app saves is the location of mkvpropedit. On Windows it's stored in `%APPDATA%\JMkvpropedit\`.

## Building from source

See [BUILDING.md](BUILDING.md) for how to build, test and package the app, and how the code is organised.

## Credits and license

JMkvpropedit was created by Bruno Barbieri ([BrunoReX/jmkvpropedit](https://github.com/BrunoReX/jmkvpropedit)). The code was updated to modern Java by sergeloie ([sergeloie/jmkvpropedit](https://github.com/sergeloie/jmkvpropedit)).

JMkvpropedit is free software under a BSD-style license; see [license.txt](license.txt). It relies on [MKVToolNix](https://mkvtoolnix.download/) by Moritz Bunkus, which is installed separately.
