package com.mahghuuuls.balancekit.rules;

import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.config.EntrySpec;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Owns which entries apply to a mob and how they combine.
 *
 * <ul>
 *   <li>An entry without an NBT condition applies to every mob of its entity ID, checked at every load.</li>
 *   <li>An entry with a condition applies only to mobs whose first balance matched it. The match is remembered by
 *   the entry's fingerprint (list, entity ID and the condition in a canonical text form), so reordering the list
 *   keeps matches, and editing or deleting the condition drops them.</li>
 *   <li>Each applicable rule multiplies once; order does not matter. A blocklist wins over what it covers.</li>
 * </ul>
 */
public final class RuleBook {

    private final double globalHealth;
    private final VariationRange variation;
    // Global mob damage and movement multipliers arrive with their settings; until then they are neutral.
    private final double globalDealt = 1.0D;
    private final double globalReceived = 1.0D;
    private final double globalMovement = 1.0D;
    private final Map<String, List<Entry>> byEntity = new HashMap<>();

    public RuleBook(BalanceSettings settings) {
        this.globalHealth = settings.mobHealth();
        this.variation = new VariationRange(settings.variationLower(), settings.variationUpper());
        add(settings.entityRules(), Kind.RULE);
        add(settings.healthBlocklist(), Kind.HEALTH_BLOCK);
        add(settings.variationBlocklist(), Kind.VARIATION_BLOCK);
        add(settings.damageBlocklist(), Kind.DAMAGE_BLOCK);
        add(settings.movementBlocklist(), Kind.MOVEMENT_BLOCK);
    }

    private void add(List<EntrySpec> specs, Kind kind) {
        for (EntrySpec spec : specs) {
            byEntity.computeIfAbsent(spec.entityId(), k -> new ArrayList<>()).add(new Entry(spec, kind));
        }
    }

    /** The entity IDs any entry names, for the startup check against the registry. */
    public Set<String> entityIds() {
        return Collections.unmodifiableSet(byEntity.keySet());
    }

    /** True when some entry for this entity ID has an NBT condition, so first balance must read the mob's NBT. */
    private boolean needsNbt(String entityId) {
        for (Entry entry : entries(entityId)) {
            if (entry.spec.condition() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fingerprints of the conditioned entries the mob matches at its first balance. Reads the NBT only when a
     * conditioned entry exists for the entity ID.
     */
    public Set<String> evaluateFirst(String entityId, Supplier<NBTTagCompound> nbt) {
        Set<String> matches = new TreeSet<>();
        if (!needsNbt(entityId)) {
            return matches;
        }
        NBTTagCompound actual = nbt.get();
        for (Entry entry : entries(entityId)) {
            if (entry.spec.condition() != null && NBTUtil.areNBTEquals(entry.spec.condition(), actual, true)) {
                matches.add(entry.fingerprint);
            }
        }
        return matches;
    }

    /**
     * What applies to a mob of this entity ID with these saved matches and this variation roll (NaN for none),
     * under the current config.
     */
    public MobProfile profile(String entityId, Set<String> savedMatches, double roll) {
        double ruleHealth = 1.0D;
        double ruleDealt = 1.0D;
        double ruleReceived = 1.0D;
        double ruleMovement = 1.0D;
        boolean healthBlocked = false;
        boolean variationBlocked = false;
        boolean damageBlocked = false;
        boolean movementBlocked = false;
        Set<String> applied = new LinkedHashSet<>();
        for (Entry entry : entries(entityId)) {
            if (entry.spec.condition() != null && !savedMatches.contains(entry.fingerprint)) {
                continue;
            }
            applied.add(entry.describe());
            switch (entry.kind) {
                case RULE:
                    ruleHealth *= orOne(entry.spec.health());
                    ruleDealt *= orOne(entry.spec.dealt());
                    ruleReceived *= orOne(entry.spec.received());
                    ruleMovement *= orOne(entry.spec.movement());
                    break;
                case HEALTH_BLOCK:
                    healthBlocked = true;
                    break;
                case VARIATION_BLOCK:
                    variationBlocked = true;
                    break;
                case DAMAGE_BLOCK:
                    damageBlocked = true;
                    break;
                case MOVEMENT_BLOCK:
                    movementBlocked = true;
                    break;
                default:
                    throw new IllegalStateException(entry.kind.name());
            }
        }
        boolean noVariation = healthBlocked || variationBlocked || Double.isNaN(roll);
        double variationMultiplier = noVariation ? 1.0D : variation.multiplier(roll);
        double health = healthBlocked ? 1.0D : globalHealth * ruleHealth * variationMultiplier;
        double dealt = damageBlocked ? 1.0D : globalDealt * ruleDealt;
        double received = damageBlocked ? 1.0D : globalReceived * ruleReceived;
        double movement = movementBlocked ? 1.0D : globalMovement * ruleMovement;
        return new MobProfile(health, variationMultiplier, dealt, received, movement, healthBlocked,
                healthBlocked || variationBlocked, damageBlocked, movementBlocked, new ArrayList<>(applied));
    }

    private List<Entry> entries(String entityId) {
        List<Entry> entries = byEntity.get(entityId);
        return entries == null ? Collections.<Entry>emptyList() : entries;
    }

    private static double orOne(double value) {
        return Double.isNaN(value) ? 1.0D : value;
    }

    /**
     * Text that is the same for conditions that match the same entities: compound keys and list elements are
     * sorted (list matching ignores order) and keys that need it are quoted. {@code NBTTagCompound.toString()} is not
     * used because its key order follows the hash map, not the text.
     */
    static String canonical(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (String key : new TreeSet<>(compound.getKeySet())) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(quoteKey(key)).append(':').append(canonical(compound.getTag(key)));
            }
            return sb.append('}').toString();
        }
        if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            List<String> elements = new ArrayList<>();
            for (int i = 0; i < list.tagCount(); i++) {
                elements.add(canonical(list.get(i)));
            }
            Collections.sort(elements);
            return "[" + String.join(",", elements) + "]";
        }
        return tag.toString();
    }

    private static String quoteKey(String key) {
        if (key.matches("[A-Za-z0-9._+-]+")) {
            return key;
        }
        return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private enum Kind {
        RULE, HEALTH_BLOCK, VARIATION_BLOCK, DAMAGE_BLOCK, MOVEMENT_BLOCK
    }

    private static final class Entry {

        final EntrySpec spec;
        final Kind kind;
        final String fingerprint;

        Entry(EntrySpec spec, Kind kind) {
            this.spec = spec;
            this.kind = kind;
            this.fingerprint = spec.list() + "|" + spec.entityId() + "|"
                    + (spec.condition() == null ? "" : canonical(spec.condition()));
        }

        String describe() {
            return spec.list() + " line " + spec.line() + ": " + spec.text();
        }
    }
}
