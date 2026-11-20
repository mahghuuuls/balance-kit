# Balance Kit

Balance Kit scales mob and player health, damage and movement speed, player attack speed and underwater air time from one config file, `config/balancekit.cfg`. With its default settings it changes nothing.

Player-facing description: [MOD-PAGE.md](MOD-PAGE.md). Release history: [CHANGELOG.md](CHANGELOG.md).

## Identity

- Mod ID `balancekit`, Minecraft 1.12.2, Forge. Tested with Forge 14.23.5.2864 and with Cleanroom 0.7.14-alpha. No other mod is required.
- Install on the server. Clients can join without it; a client with it uses the server's breathing multiplier for its own air display.
- Config changes take effect after a restart.

## Config Sections

- `mobs`: `health`, `damageDealt`, `damageReceived`, `movementSpeed`, `variationLower`, `variationUpper`. A mob is every non-player creature, vanilla or modded.
- `player`: `baseHealthEnabled`, `baseHealth`, `damageDealt`, `damageReceived`, `movementSpeed`, `attackSpeed`, `breathingDuration`. Fake players get no change.
- `rules`: `entityRules`, one per line: `<entity id> [<NBT condition>] [health=<n>] [dealt=<n>] [received=<n>] [movement=<n>]`. Every matching rule multiplies with the global value; the NBT condition is checked once, when the mob is first balanced.
- `blocklists`: `health`, `damage`, `movement`, `variation`, one `<entity id> [<NBT condition>]` per line.
- `damage`: `excludedDamageTypes`, damage type names that are never scaled. `outOfWorld` is never scaled anyway.
- `diagnostics`: `logDamageTypes` writes one server log line per damage event.
- `compat`: `removeScalingHealthModifiers` removes the max health and attack damage modifiers Scaling Health 1.3.42 leaves in saves after it is removed.

The generated file's comments give the full rules for each value.

## Behavior Notes For Pack Authors

- Health and attribute changes are applied as unsaved modifiers on the underlying base value, so removing the mod or changing a value takes effect the next time an entity loads. Other mods' modifiers are kept.
- The damage blocklist skips only the listed mob's own dealt and received multipliers; the attacker's or victim's multipliers still apply.
- Damage multipliers apply before armor. The attacker is the damage source's true source: a player's arrows, TNT and thorns use the player's dealt multiplier.
- `/balancekit inspect [player or selector]` (permission level 2 and the console) reports the underlying and adjusted values, multipliers, matched rules and blocklists. Without a target, an operator inspects the entity under the crosshair.

## License

[MIT License](LICENSE). See [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) for template notices.
