package ru.anseranser.jmkvpropedit;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the free-form "Extra parameters" fields into mkvpropedit arguments.
 *
 * <p>
 * Rules, chosen so that ordinary text survives untouched:
 * </p>
 * <ul>
 * <li>Whitespace separates arguments.</li>
 * <li>Double quotes group text anywhere in an argument:
 * {@code --set "title=A B"} and {@code --set title="A B"} both give
 * {@code title=A B}. Inside double quotes, {@code \"} is a literal quote;
 * every other backslash is literal, so Windows paths need no escaping.</li>
 * <li>Single quotes group text only when they open an argument
 * ({@code 'A B'}). Elsewhere an apostrophe is an ordinary character, so
 * {@code name=Director's} stays intact.</li>
 * <li>An unclosed quote is an error rather than being silently dropped.</li>
 * </ul>
 * <p>
 * One limitation: a backslash directly before a closing double quote reads as
 * an escaped quote, so a quoted folder path cannot end in a backslash.
 * </p>
 */
public final class ArgumentSplitter {

    /** Thrown when the text cannot be split (unclosed quote). */
    public static final class InvalidParametersException extends InvalidSettingException {
        private static final long serialVersionUID = 1L;

        InvalidParametersException(String message) {
            super(message);
        }
    }

    private ArgumentSplitter() {
    }

    /**
     * @param text the user's text; {@code null} or blank gives no arguments
     * @return the arguments, in order
     * @throws InvalidParametersException on an unclosed quote
     */
    public static List<String> split(String text) {
        List<String> args = new ArrayList<>();

        if (text == null) {
            return args;
        }

        StringBuilder current = new StringBuilder();
        boolean inArg = false;
        char quote = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (quote == '"') {
                if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quote = 0;
                } else {
                    current.append(c);
                }
            } else if (quote == '\'') {
                if (c == '\'') {
                    quote = 0;
                } else {
                    current.append(c);
                }
            } else if (Character.isWhitespace(c)) {
                if (inArg) {
                    args.add(current.toString());
                    current.setLength(0);
                    inArg = false;
                }
            } else if (c == '"') {
                quote = '"';
                inArg = true;
            } else if (c == '\'' && !inArg) {
                quote = '\'';
                inArg = true;
            } else {
                current.append(c);
                inArg = true;
            }
        }

        if (quote != 0) {
            throw new InvalidParametersException("Unclosed " + (quote == '"' ? "double" : "single")
                    + " quote in: " + text);
        }

        if (inArg) {
            args.add(current.toString());
        }

        return args;
    }
}
