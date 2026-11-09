package ru.anseranser.jmkvpropedit;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders an argument list as a single command line for display ("Generate
 * command line" and the Output log). Display only: the real run passes the
 * argument list straight to mkvpropedit through the options file, so this
 * text never gets re-parsed by the application.
 *
 * <p>
 * The quoting matches the platform's shell so the line can be copied and
 * pasted: double quotes on Windows (cmd.exe / PowerShell), single quotes on
 * POSIX shells.
 * </p>
 */
public final class CommandLineFormatter {

    private static final Pattern SAFE_POSIX = Pattern.compile("[A-Za-z0-9_+=:,./@%^-]+");
    private static final Pattern SAFE_WINDOWS = Pattern.compile("[A-Za-z0-9_+=:,./@\\\\-]+");

    private CommandLineFormatter() {
    }

    public static String format(List<String> args, boolean windows) {
        StringBuilder sb = new StringBuilder();

        for (String arg : args) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(quote(arg, windows));
        }

        return sb.toString();
    }

    public static String quote(String arg, boolean windows) {
        if (windows) {
            if (!arg.isEmpty() && SAFE_WINDOWS.matcher(arg).matches()) {
                return arg;
            }
            return "\"" + quoteWindows(arg) + "\"";
        }

        if (!arg.isEmpty() && SAFE_POSIX.matcher(arg).matches()) {
            return arg;
        }
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    /**
     * Microsoft C runtime rules: a quote is escaped with a backslash, and any
     * backslashes directly before a quote (or before the closing quote) are
     * doubled. Other backslashes stay as they are.
     */
    private static String quoteWindows(String arg) {
        StringBuilder sb = new StringBuilder();
        int backslashes = 0;

        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);

            if (c == '\\') {
                backslashes++;
                continue;
            }

            if (c == '"') {
                sb.append("\\".repeat(backslashes * 2 + 1)).append('"');
            } else {
                sb.append("\\".repeat(backslashes)).append(c);
            }
            backslashes = 0;
        }

        return sb.append("\\".repeat(backslashes * 2)).toString();
    }
}
