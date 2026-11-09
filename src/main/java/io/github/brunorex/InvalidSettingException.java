package io.github.brunorex;

/**
 * A setting the user entered cannot be turned into mkvpropedit arguments
 * (unclosed quote in "Extra parameters", malformed language tag, ...). The
 * message is shown to the user as is.
 */
public class InvalidSettingException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidSettingException(String message) {
        super(message);
    }
}
