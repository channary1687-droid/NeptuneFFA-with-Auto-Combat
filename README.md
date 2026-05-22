# Neptune FFA Addon

Standalone addon for Neptune Practice Core that adds persistent FFA arenas backed by Neptune kits.

## Build

```bash
mvn -DskipTests package
```

The jar is written to:

```text
target/NeptuneFFAAddon-1.0.0.jar
```

## Install

1. Install Neptune on the Paper server.
2. Place `NeptuneFFAAddon-1.0.0.jar` in the server `plugins/` folder.
3. Restart the server.

## Commands

- `/ffa` - open the configurable arena selector GUI.
- `/ffa editor` - open the arena editor GUI. Requires `neptuneffa.admin`.
- `/ffa <arena>` - join an arena with its linked kit.
- `/ffa join <arena> <kit>` - join a persistent FFA arena.
- `/ffa leave` - leave the current FFA arena.
- `/ffa list` - list configured FFA arenas.
- `/ffa stats` - view your FFA progression stats.
- `/ffa top <kills|deaths|streak|coins|level|season>` - view leaderboards.
- `/ffa quests` - view quest progress.
- `/ffa battlepass` or `/ffa bp` - view battlepass progress.
- `/ffa season` - view the active season.
- `/ffa reload` - reload config and arenas. Requires `neptuneffa.admin`.
- `/ffa event <type|stop>` - force or stop an FFA event. Requires `neptuneffa.admin`.
- `/ffa bounty <player>` - force a bounty target. Requires `neptuneffa.admin`.
- `/ffa rotate` - manually rotate FFA arenas. Requires `neptuneffa.admin`.
- `/ffa arena create <name>` - create an addon FFA arena.
- `/ffa arena delete <name>` - delete an addon FFA arena.
- `/ffa arena pos1 <name>` - set the arena region position 1.
- `/ffa arena pos2 <name>` - set the arena region position 2.
- `/ffa arena setspawn <name>` - set the player spawn for an FFA arena. The spawn is centered on the block and creates a 3x3 PvP-safe zone.
- `/ffa arena slot <name> <slot|auto>` - set the arena selector slot used by `/ffa`.
- `/ffa arena icon <name> [material]` - set the arena selector icon. Without material, uses the item in your hand.
- `/ffa arena flag <name> <allow-place|allow-break|auto-regen> <allow|deny>` - update arena gameplay flags.
- `/ffa arena link <name> <kit>` - link a Neptune kit to the FFA arena.
- `/ffa arena unlink <name> <kit>` - unlink a Neptune kit.

Arena management commands require `neptuneffa.admin`.

## Notes

The addon keeps Neptune kits as the source of truth. When a player joins, respawns, or gets a kill, it clears effects, repairs armor/tools, resets health/food/exp, and reapplies the selected Neptune kit loadout.

Neptune kit flags are respected in FFA. The `resetArenaAfterMatch` kit flag regenerates tracked FFA arena block changes after deaths, which is the persistent-FFA equivalent of round arena reset.

## Professional Systems

Implemented foundation:

- combat metrics: CPS, reach, accuracy, combo, damage, headshots, critical hits
- persistent stats: kills, deaths, streaks, coins, XP, level, prestige fields
- killstreak rewards: speed, coins, broadcast, bounty, supply drop effect, champion status
- random event engine: double damage, no knockback, explosive arrows, infinite healing, one shot, speed FFA
- hotzone multipliers and glowing
- bounty target glow, bossbar, sound, tracker compass, claim rewards
- scoreboard, actionbar, and event/bounty bossbar UI
- basic combat-log punishment on quit
- quests: daily/weekly progress rewards
- battlepass tiers and rewards
- season points and season command
- leaderboards
- anti-camp detection and reward reduction
- powerup spawning and pickup effects
- King of the Hill center capture
- automatic map rotation

Not implemented yet:

- persistent quest reset windows
- AI bots
- replay/killcam storage
- season reset pipeline
- network module split such as FFA-Core, FFA-API, FFA-Stats
