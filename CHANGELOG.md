# Changelog

## 1.0.0

### Added

- Mob multipliers for max health, damage dealt, damage received and movement speed, applied to every non-player creature.
- Random health variation: each mob gets a lasting health roll between `variationLower` and `variationUpper`.
- Entity rules for single mob types or NBT variants, multiplying with the global values.
- Blocklists for health, damage, movement and variation.
- Player settings: base max health, damage dealt and received, walking speed, attack speed and breathing duration.
- Damage type exclusions and an optional damage log that names each damage type.
- `/balancekit inspect` for players, mobs and the server console.
- Optional cleanup of the modifiers Scaling Health 1.3.42 leaves in saves after it is removed.

### Compatibility

- Server-side install; clients can join without the mod.
- Changes are not written into saves; removing the mod returns entities to their normal values.
