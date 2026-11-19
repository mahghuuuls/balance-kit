package com.mahghuuuls.balancekit.config;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Owns the one-line entry syntax of the rule and blocklist settings:
 * {@code <entity id> [<nbt condition>] [health=<n>] [dealt=<n>] [received=<n>] [movement=<n>]}.
 * The condition is the balanced {...} block after the ID, in /summon syntax. A line with a problem is skipped and
 * reported; blank lines are ignored.
 */
public final class EntryParser {

    private static final Pattern ENTITY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private EntryParser() {
    }

    /** Parses every line of one list. {@code multipliersAllowed} is false for blocklists. */
    public static List<EntrySpec> parse(String list, String[] lines, boolean multipliersAllowed,
                                        List<ConfigWarning> warnings) {
        List<EntrySpec> entries = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String text = lines[i] == null ? "" : lines[i].trim();
            if (text.isEmpty()) {
                continue;
            }
            String location = list + " line " + (i + 1);
            try {
                entries.add(parseLine(list, i + 1, text, multipliersAllowed));
            } catch (IllegalArgumentException e) {
                warnings.add(new ConfigWarning(location, text, e.getMessage() + "; entry skipped"));
            }
        }
        return entries;
    }

    static EntrySpec parseLine(String list, int line, String text, boolean multipliersAllowed) {
        int idEnd = 0;
        while (idEnd < text.length() && !Character.isWhitespace(text.charAt(idEnd)) && text.charAt(idEnd) != '{') {
            idEnd++;
        }
        String rawId = text.substring(0, idEnd);
        String entityId = rawId.indexOf(':') < 0 ? "minecraft:" + rawId : rawId;
        entityId = entityId.toLowerCase(Locale.ROOT);
        if (rawId.isEmpty() || !ENTITY_ID.matcher(entityId).matches()) {
            throw new IllegalArgumentException("does not start with an entity ID such as minecraft:zombie");
        }
        int pos = skipSpaces(text, idEnd);
        NBTTagCompound condition = null;
        if (pos < text.length() && text.charAt(pos) == '{') {
            int end = conditionEnd(text, pos);
            String conditionText = text.substring(pos, end);
            try {
                condition = JsonToNBT.getTagFromJson(conditionText);
            } catch (NBTException e) {
                throw new IllegalArgumentException("has an NBT condition that cannot be read (" + e.getMessage() + ")");
            }
            pos = end;
        }
        double health = Double.NaN;
        double dealt = Double.NaN;
        double received = Double.NaN;
        double movement = Double.NaN;
        String rest = text.substring(pos).trim();
        if (!rest.isEmpty()) {
            if (!multipliersAllowed) {
                throw new IllegalArgumentException("is a blocklist entry, which takes no multipliers");
            }
            for (String token : rest.split("\\s+")) {
                int eq = token.indexOf('=');
                if (eq <= 0) {
                    throw new IllegalArgumentException("has '" + token + "', expected key=value");
                }
                String key = token.substring(0, eq);
                double value = number(token.substring(eq + 1), key);
                switch (key) {
                    case "health":
                        health = once(health, positive(value, key), key);
                        break;
                    case "dealt":
                        dealt = once(dealt, notNegative(value, key), key);
                        break;
                    case "received":
                        received = once(received, notNegative(value, key), key);
                        break;
                    case "movement":
                        movement = once(movement, positive(value, key), key);
                        break;
                    default:
                        throw new IllegalArgumentException("has an unknown key '" + key
                                + "' (known: health, dealt, received, movement)");
                }
            }
        }
        return new EntrySpec(list, line, text, entityId, condition, health, dealt, received, movement);
    }

    private static int skipSpaces(String text, int pos) {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    /** Index just after the brace that closes the one at {@code start}; braces inside quotes do not count. */
    private static int conditionEnd(String text, int start) {
        int depth = 0;
        boolean quoted = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quoted = false;
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        throw new IllegalArgumentException("has an NBT condition without its closing brace");
    }

    private static double number(String text, String key) {
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("has " + key + "=" + text + ", which is not a finite number");
        }
    }

    private static double positive(double value, String key) {
        if (value <= 0.0D) {
            throw new IllegalArgumentException("has " + key + " at or below 0; it must be above 0");
        }
        return value;
    }

    private static double notNegative(double value, String key) {
        if (value < 0.0D) {
            throw new IllegalArgumentException("has " + key + " below 0; it must be 0 or more");
        }
        return value;
    }

    private static double once(double previous, double value, String key) {
        if (!Double.isNaN(previous)) {
            throw new IllegalArgumentException("sets " + key + " twice");
        }
        return value;
    }
}
