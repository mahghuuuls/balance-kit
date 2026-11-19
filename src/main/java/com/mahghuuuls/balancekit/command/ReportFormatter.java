package com.mahghuuuls.balancekit.command;

import com.mahghuuuls.balancekit.mob.HealthReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the plain-text lines of an inspection report. Plain text, not translation keys, so a client without
 * Balance Kit still reads it.
 */
public final class ReportFormatter {

    private ReportFormatter() {
    }

    /** One header line naming the entity, then the section lines indented beneath it. */
    public static List<String> report(String entityId, String name, int runtimeId, List<String> sections) {
        List<String> lines = new ArrayList<>();
        String label = name == null || name.isEmpty() ? "" : " \"" + name + "\"";
        lines.add("Balance Kit: " + entityId + label + " (entity " + runtimeId + ")");
        for (String section : sections) {
            lines.add("  " + section);
        }
        return lines;
    }

    /** The mob and max health lines. */
    public static List<String> health(HealthReport report) {
        List<String> lines = new ArrayList<>();
        if (!report.balanced()) {
            lines.add("Mob: yes; first balance pending (it happens on the mob's first server tick).");
            return lines;
        }
        lines.add("Mob: yes");
        lines.add("Max health: underlying base " + number(report.underlyingBase()) + ", adjusted base "
                + number(report.adjustedBase()) + " (x" + number(report.appliedFactor()) + "), result "
                + number(report.maxHealth()) + "; health " + number(report.health()));
        if (!report.inEffect()) {
            lines.add("Not yet in effect: the config asks for x" + number(report.configuredFactor()) + ".");
        }
        if (report.capped()) {
            lines.add("Cap: the max health cap of " + number(report.cap()) + " applies.");
        }
        return lines;
    }

    /** One line per applicable rule or blocklist entry, or a line saying there is none. */
    public static List<String> entries(List<String> applied) {
        List<String> lines = new ArrayList<>();
        if (applied.isEmpty()) {
            lines.add("Rules and blocklists: none apply.");
        }
        for (String entry : applied) {
            lines.add("Applies: " + entry);
        }
        return lines;
    }

    /** A number with up to three decimals and no trailing zeros, so 40.0 reads "40" and 0.345 reads "0.345". */
    public static String number(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        String text = String.format(Locale.ROOT, "%.3f", value);
        if (text.contains(".")) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text;
    }
}
