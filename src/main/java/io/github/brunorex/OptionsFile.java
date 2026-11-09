package io.github.brunorex;

import java.util.List;

/**
 * Serializes mkvpropedit arguments into the JSON array format that
 * {@code mkvpropedit @file.json} reads. Each argument goes in verbatim; no
 * shell-style quoting is involved.
 */
public final class OptionsFile {

    private OptionsFile() {
    }

    public static String json(List<String> args) {
        StringBuilder json = new StringBuilder("[\n");

        for (int i = 0; i < args.size(); i++) {
            json.append("  ").append(jsonString(args.get(i)));
            if (i < args.size() - 1) {
                json.append(',');
            }
            json.append('\n');
        }

        return json.append("]\n").toString();
    }

    /**
     * Minimal JSON string writer: escapes the quote, backslash and control
     * characters; everything else (including unicode) is valid UTF-8 JSON as-is.
     */
    static String jsonString(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        sb.append('"');

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
            case '"' -> sb.append("\\\"");
            case '\\' -> sb.append("\\\\");
            case '\b' -> sb.append("\\b");
            case '\f' -> sb.append("\\f");
            case '\n' -> sb.append("\\n");
            case '\r' -> sb.append("\\r");
            case '\t' -> sb.append("\\t");
            default -> {
                if (c < 0x20) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    sb.append(c);
                }
            }
            }
        }

        return sb.append('"').toString();
    }
}
