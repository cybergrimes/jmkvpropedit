package ru.anseranser.jmkvpropedit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds mkvpropedit argument lists from plain settings.
 *
 * <p>
 * Every value is kept as its own argument from start to finish: nothing is
 * joined into a command string and split again, so titles, names and paths
 * containing quotes, apostrophes, backslashes or spaces reach mkvpropedit
 * exactly as typed. The only text that gets split is the free-form "Extra
 * parameters" field, via {@link ArgumentSplitter}. The human-readable command
 * line is rendered from the same list by {@link CommandLineFormatter}, so the
 * preview always matches what runs.
 * </p>
 *
 * <p>
 * This class never touches Swing — the caller reads the widgets into the
 * settings records below.
 * </p>
 */
public final class CommandBuilder {

    /**
     * Tags/chapters source selector.
     *
     * @param enabled   the section checkbox
     * @param mode      0 = Remove, 1 = From file, 2 = Match file name with suffix
     * @param text      the text field content
     * @param extension the extension combo content (.xml/.txt), used by mode 2
     */
    public record SourceSetting(boolean enabled, int mode, String text, String extension) {
    }

    /**
     * The segment title setting. Numbering fields stay raw text and are parsed
     * only when numbering is actually used.
     *
     * @param enabled     the title checkbox
     * @param numbering   the numbering checkbox
     * @param text        the title text ({@code {num}}/{@code {file_name}} templates)
     * @param numberStart raw text of the numbering start field
     * @param numberPad   raw text of the numbering padding field
     */
    public record TitleSetting(boolean enabled, boolean numbering, String text,
            String numberStart, String numberPad) {
    }

    /**
     * The General tab as plain data: one settings object for the whole file list.
     *
     * @param tags         tags section
     * @param chapters     chapters section
     * @param title        title section
     * @param extraEnabled the "Extra parameters" checkbox
     * @param extra        the extra parameters text
     */
    public record GeneralSettings(SourceSetting tags, SourceSetting chapters,
            TitleSetting title, boolean extraEnabled, String extra) {
    }

    /**
     * One track slot (video/audio/subtitle) as plain data. Track types share
     * this shape; the track type itself is the {@code v}/{@code a}/{@code s}
     * selector passed to {@link #buildTracks}.
     *
     * @param edit        the "Edit this track" checkbox
     * @param flags       the flags to set (ticked checkboxes only), mapped to
     *                    their Yes ({@code true}) / No ({@code false}) value;
     *                    emitted in {@link TrackFlag} order
     * @param setName     append the track name
     * @param name        the name text ({@code {num}}/{@code {file_name}} templates)
     * @param setLanguage append the language
     * @param language    the language: an ISO 639 code from the list or a BCP 47 tag
     * @param setExtra    append the extra parameters
     * @param extra       the extra parameters text
     * @param numbering   the per-track numbering checkbox
     * @param numberStart raw text of the numbering start field
     * @param numberPad   raw text of the numbering padding field
     */
    public record TrackSettings(boolean edit, Map<TrackFlag, Boolean> flags,
            boolean setName, String name,
            boolean setLanguage, String language,
            boolean setExtra, String extra,
            boolean numbering, String numberStart, String numberPad) {

        public TrackSettings {
            EnumMap<TrackFlag, Boolean> copy = new EnumMap<>(TrackFlag.class);
            if (flags != null) {
                copy.putAll(flags);
            }
            flags = Collections.unmodifiableMap(copy);
        }

        public boolean sets(TrackFlag flag) {
            return flags.containsKey(flag);
        }

        public boolean value(TrackFlag flag) {
            return Boolean.TRUE.equals(flags.get(flag));
        }

        public boolean setEnabled() {
            return sets(TrackFlag.ENABLED);
        }

        public boolean enableValue() {
            return value(TrackFlag.ENABLED);
        }

        public boolean setDefault() {
            return sets(TrackFlag.DEFAULT);
        }

        public boolean defaultValue() {
            return value(TrackFlag.DEFAULT);
        }

        public boolean setForced() {
            return sets(TrackFlag.FORCED);
        }

        public boolean forcedValue() {
            return value(TrackFlag.FORCED);
        }
    }

    /**
     * Per-file arguments for one section (General, Video, ...), in file order.
     *
     * @param perFile one unmodifiable argument list per file
     */
    public record Section(List<List<String>> perFile) {

        public List<String> forFile(int index) {
            return perFile.get(index);
        }

        public int size() {
            return perFile.size();
        }
    }

    /**
     * The finished batch, one entry per file, same order as the file list.
     *
     * @param lines display command lines (exe + file + arguments, shell-quoted)
     * @param args  the real arguments for each file (file + edit arguments,
     *              without the executable) — what goes into the options file
     */
    public record Batch(List<String> lines, List<List<String>> args) {
    }

    private final boolean windows;

    /** Uses the current platform's quoting for the display lines. */
    public CommandBuilder() {
        this(Utils.isWindows());
    }

    /** Test seam: pins the platform used for display-line quoting. */
    public CommandBuilder(boolean windows) {
        this.windows = windows;
    }

    /**
     * Builds the General-tab arguments, one list per file: tags, chapters,
     * title (with {@code {num}}/{@code {file_name}} substitution) and extra
     * parameters, in that order.
     *
     * @throws NumberFormatException                        if title numbering is on
     *                                                      and a numbering field is
     *                                                      not a number
     * @throws ArgumentSplitter.InvalidParametersException if the extra parameters
     *                                                      have an unclosed quote
     */
    public Section buildGeneral(List<String> files, GeneralSettings settings) {
        TitleSetting title = settings.title();
        boolean numbered = title.enabled() && title.numbering();
        int number = numbered ? Integer.parseInt(title.numberStart().trim()) : 0;
        int pad = numbered ? Integer.parseInt(title.numberPad().trim()) : 0;

        List<String> extra = settings.extraEnabled() ? ArgumentSplitter.split(settings.extra()) : List.of();

        List<List<String>> perFile = new ArrayList<>(files.size());

        for (String file : files) {
            List<String> args = new ArrayList<>();

            appendSource(args, "--tags", "all:", settings.tags(), file);
            appendSource(args, "--chapters", "", settings.chapters(), file);

            if (title.enabled()) {
                String newTitle = title.text();

                if (numbered) {
                    newTitle = newTitle.replace("{num}", Utils.padNumber(pad, number));
                    number++;
                }

                newTitle = newTitle.replace("{file_name}", Utils.getFileNameWithoutExt(file));

                args.add("--edit");
                args.add("info");
                args.add("--set");
                args.add("title=" + newTitle);
            }

            args.addAll(extra);
            perFile.add(Collections.unmodifiableList(args));
        }

        return new Section(Collections.unmodifiableList(perFile));
    }

    /**
     * Tags and chapters share one shape: an option whose value is either
     * empty (remove), a fixed file, or the media file's path with a suffix.
     * Tags prefix the value with {@code all:}.
     */
    private static void appendSource(List<String> args, String option, String prefix,
            SourceSetting source, String file) {
        if (!source.enabled()) {
            return;
        }

        String value = switch (source.mode()) {
        case 0 -> "";
        case 1 -> source.text().trim().isEmpty() ? "" : source.text();
        case 2 -> Utils.getPathWithoutExt(file) + source.text() + source.extension();
        default -> null;
        };

        if (value != null) {
            args.add(option);
            args.add(prefix + value);
        }
    }

    /**
     * Builds the track (video/audio/subtitle) arguments, one list per file.
     * A track is emitted only when "Edit this track" is on <em>and</em> at
     * least one property is set for that track; a bare {@code --edit track:aN}
     * would make mkvpropedit fail on files that lack that track.
     *
     * @param files    the file list, in order
     * @param selector the track selector: {@code v}, {@code a} or {@code s}
     * @param tracks   the track slots, slot index = track number - 1
     * @throws NumberFormatException                        if an edited, numbered
     *                                                      track has a non-numeric
     *                                                      numbering field
     * @throws InvalidSettingException                    if a track's extra
     *                                                      parameters have an
     *                                                      unclosed quote or its
     *                                                      language tag is malformed
     */
    public Section buildTracks(List<String> files, char selector, List<TrackSettings> tracks) {
        List<List<String>> templates = new ArrayList<>(tracks.size());
        int[] numbers = new int[tracks.size()];
        int[] pads = new int[tracks.size()];

        for (int t = 0; t < tracks.size(); t++) {
            TrackSettings settings = tracks.get(t);
            List<String> template = trackTemplate(selector, t + 1, settings);
            templates.add(template);

            if (!template.isEmpty() && settings.numbering()) {
                numbers[t] = Integer.parseInt(settings.numberStart().trim());
                pads[t] = Integer.parseInt(settings.numberPad().trim());
            }
        }

        List<List<String>> perFile = new ArrayList<>(files.size());

        for (String file : files) {
            List<String> args = new ArrayList<>();
            String fileName = Utils.getFileNameWithoutExt(file);

            for (int t = 0; t < tracks.size(); t++) {
                List<String> template = templates.get(t);

                if (template.isEmpty()) {
                    continue;
                }

                String num = null;
                if (tracks.get(t).numbering()) {
                    num = Utils.padNumber(pads[t], numbers[t]);
                    numbers[t]++;
                }

                for (String arg : template) {
                    if (num != null) {
                        arg = arg.replace("{num}", num);
                    }
                    args.add(arg.replace("{file_name}", fileName));
                }
            }

            perFile.add(Collections.unmodifiableList(args));
        }

        return new Section(Collections.unmodifiableList(perFile));
    }

    /** The arguments for one track before per-file substitution; empty if nothing to do. */
    private static List<String> trackTemplate(char selector, int trackNumber, TrackSettings settings) {
        if (!settings.edit()) {
            return List.of();
        }

        List<String> props = new ArrayList<>();

        for (Map.Entry<TrackFlag, Boolean> flag : settings.flags().entrySet()) {
            addSet(props, flag.getKey().property() + "=" + (flag.getValue() ? "1" : "0"));
        }

        if (settings.setName()) {
            addSet(props, "name=" + settings.name());
        }

        if (settings.setLanguage()) {
            addSet(props, "language=" + LanguageTags.validate(settings.language()));
        }

        if (settings.setExtra()) {
            props.addAll(ArgumentSplitter.split(settings.extra()));
        }

        if (props.isEmpty()) {
            return List.of();
        }

        List<String> template = new ArrayList<>(props.size() + 2);
        template.add("--edit");
        template.add("track:" + selector + trackNumber);
        template.addAll(props);
        return template;
    }

    private static void addSet(List<String> args, String assignment) {
        args.add("--set");
        args.add(assignment);
    }

    /**
     * The arguments for one attachment operation's table rows. Rows are the
     * {@link AttachmentModel#rows()} of that operation's table.
     */
    public static List<String> attachmentArgs(AttachmentOperation operation, List<String[]> rows) {
        List<String> args = new ArrayList<>();

        for (String[] row : rows) {
            switch (operation) {
            case ADD -> {
                appendAttachmentMeta(args, row[1], row[2], row[3]);
                args.add("--add-attachment");
                args.add(row[0]);
            }
            case REPLACE -> {
                appendAttachmentMeta(args, row[3], row[4], row[5]);
                args.add("--replace-attachment");
                args.add(selectorPrefix(AttachmentSelector.fromLabel(row[0])) + row[1] + ":" + row[2]);
            }
            case DELETE -> {
                args.add("--delete-attachment");
                args.add(selectorPrefix(AttachmentSelector.fromLabel(row[0])) + row[1]);
            }
            }
        }

        return Collections.unmodifiableList(args);
    }

    private static String selectorPrefix(AttachmentSelector selector) {
        return switch (selector) {
        case NAME -> "name:";
        case ID -> "";
        case MIME_TYPE -> "mime-type:";
        };
    }

    /** The optional name/description/MIME preamble shared by add and replace rows. */
    private static void appendAttachmentMeta(List<String> args, String name, String desc, String mime) {
        if (!name.isEmpty()) {
            args.add("--attachment-name");
            args.add(name);
        }

        if (!desc.isEmpty()) {
            args.add("--attachment-description");
            args.add(desc);
        }

        if (!mime.isEmpty()) {
            args.add("--attachment-mime-type");
            args.add(mime);
        }
    }

    /**
     * Composes the final batch. An empty file list, or nothing to do for the
     * first file, yields an empty batch ("Nothing to do!").
     *
     * <p>
     * Section order: general, attachments (delete, add, replace), video,
     * audio, subtitle.
     * </p>
     *
     * @param attachments the attachment arguments, already in delete, add,
     *                    replace order (identical for every file)
     */
    public Batch buildBatch(String exePath, List<String> files, Section general, List<String> attachments,
            Section video, Section audio, Section subtitle) {
        List<String> lines = new ArrayList<>();
        List<List<String>> allArgs = new ArrayList<>();

        if (files.isEmpty()) {
            return new Batch(lines, allArgs);
        }

        for (int i = 0; i < files.size(); i++) {
            List<String> edits = new ArrayList<>();
            edits.addAll(general.forFile(i));
            edits.addAll(attachments);
            edits.addAll(video.forFile(i));
            edits.addAll(audio.forFile(i));
            edits.addAll(subtitle.forFile(i));

            if (i == 0 && edits.isEmpty()) {
                return new Batch(lines, allArgs);
            }

            List<String> args = new ArrayList<>(edits.size() + 1);
            args.add(files.get(i));
            args.addAll(edits);

            List<String> display = new ArrayList<>(args.size() + 1);
            display.add(exePath);
            display.addAll(args);

            lines.add(CommandLineFormatter.format(display, windows));
            allArgs.add(Collections.unmodifiableList(args));
        }

        return new Batch(Collections.unmodifiableList(lines), Collections.unmodifiableList(allArgs));
    }
}
