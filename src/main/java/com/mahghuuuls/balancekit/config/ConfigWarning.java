package com.mahghuuuls.balancekit.config;

/**
 * One problem found in the config file. The location names the setting (and, for list entries, the line) so a
 * pack author can find it; the text is the value as written.
 */
public final class ConfigWarning {

    private final String location;
    private final String text;
    private final String problem;

    public ConfigWarning(String location, String text, String problem) {
        this.location = location;
        this.text = text;
        this.problem = problem;
    }

    public String location() {
        return location;
    }

    public String text() {
        return text;
    }

    public String problem() {
        return problem;
    }

    @Override
    public String toString() {
        return location + ": '" + text + "' " + problem;
    }
}
