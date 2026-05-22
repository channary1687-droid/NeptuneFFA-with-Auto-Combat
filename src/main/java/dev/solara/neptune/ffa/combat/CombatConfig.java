package dev.solara.neptune.ffa.combat;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public final class CombatConfig {
    private final JavaPlugin plugin;

    public CombatConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection section(String path) {
        return plugin.getConfig().getConfigurationSection(path);
    }

    // General Settings
    public String locale() { return plugin.getConfig().getString("combat.general-settings.locale", "EN"); }
    public boolean useScoreboardTeams() { return plugin.getConfig().getBoolean("combat.general-settings.use-scoreboard-teams", true); }
    public List<String> worldExclusions() { return plugin.getConfig().getStringList("combat.general-settings.world-exclusions"); }

    // Combat Tag
    public boolean tagEnabled() { return plugin.getConfig().getBoolean("combat-tag.enabled", true); }
    public int tagTime() { return plugin.getConfig().getInt("combat-tag.time", 15); }
    public boolean tagGlowing() { return plugin.getConfig().getBoolean("combat-tag.glowing", true); }
    public boolean tagUntagOnKill() { return plugin.getConfig().getBoolean("combat-tag.untag-on-kill", false); }
    public boolean tagSelfTag() { return plugin.getConfig().getBoolean("combat-tag.self-tag", false); }
    public boolean tagEnderPearlRenew() { return plugin.getConfig().getBoolean("combat-tag.enderpearl-renews-tag", true); }
    public boolean tagWindChargeRenew() { return plugin.getConfig().getBoolean("combat-tag.windcharge-renews-tag", true); }
    public boolean tagCloseInventory() { return plugin.getConfig().getBoolean("combat-tag.close-inventory-on-tag", true); }
    public boolean tagDuelMode() { return plugin.getConfig().getBoolean("combat-tag.duel-mode", false); }

    // Display
    public boolean nametagsEnabled() { return plugin.getConfig().getBoolean("combat-tag.display.nametags.enabled", true); }
    public String nametagsPrefix() { return plugin.getConfig().getString("combat-tag.display.nametags.prefix", "&4⚔ &8(&7%pvpmanager_combat_timeleft%s&8) &c"); }
    public String nametagsSuffix() { return plugin.getConfig().getString("combat-tag.display.nametags.suffix", " &f%pvpmanager_player_health%&c❤"); }

    public boolean actionBarEnabled() { return plugin.getConfig().getBoolean("combat-tag.display.action-bar.enabled", true); }
    public String actionBarMessage() { return plugin.getConfig().getString("combat-tag.display.action-bar.message", "&8[&c&lCOMBAT&8] &7<time>s &8• &a<barsLeft>&#AA5555<barsPassed> &8• &7%pvpmanager_current_enemy% &8(&c%pvpmanager_current_enemy_health%❤&8)"); }
    public String actionBarSymbol() { return plugin.getConfig().getString("combat-tag.display.action-bar.symbol", "▊"); }
    public int actionBarTotalBars() { return plugin.getConfig().getInt("combat-tag.display.action-bar.total-bars", 20); }

    public boolean bossBarEnabled() { return plugin.getConfig().getBoolean("combat-tag.display.boss-bar.enabled", true); }
    public String bossBarMessage() { return plugin.getConfig().getString("combat-tag.display.boss-bar.message", "&8&l[&c&lCOMBAT&8&l] &e&l<time> seconds"); }
    public BarColor bossBarColor() { 
        try {
            return BarColor.valueOf(plugin.getConfig().getString("combat-tag.display.boss-bar.bar-color", "RED").toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.RED;
        }
    }
    public BarStyle bossBarStyle() { 
        try {
            return BarStyle.valueOf(plugin.getConfig().getString("combat-tag.display.boss-bar.bar-style", "SOLID").toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    // Actions Blocked
    public boolean blockEnderPearls() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.enderpearls", false); }
    public boolean blockChorusFruits() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.chorus-fruits", false); }
    public boolean blockTeleport() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.teleport", true); }
    public boolean blockUnsafeTeleports() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.unsafe-teleports", false); }
    public boolean blockEat() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.eat", false); }
    public boolean blockTotems() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.totem-of-undying", false); }
    public boolean blockPlace() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.place-blocks", false); }
    public boolean blockBreak() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.break-blocks", false); }
    public boolean blockOpenInv() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.open-inventory", false); }
    public boolean blockGliding() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.elytra.block-gliding", false); }
    public boolean blockFireworks() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.elytra.block-fireworks", false); }
    public int fireworkPowerLimit() { return plugin.getConfig().getInt("combat-tag.actions-blocked.elytra.firework-power-limit", -1); }
    public int fireworkCooldown() { return plugin.getConfig().getInt("combat-tag.actions-blocked.elytra.firework-cooldown", 2); }
    
    public boolean interactEnabled() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.interact.enabled", false); }
    public List<String> interactList() { return plugin.getConfig().getStringList("combat-tag.actions-blocked.interact.list"); }
    
    public boolean commandsEnabled() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.commands.enabled", true); }
    public boolean commandsWhitelist() { return plugin.getConfig().getBoolean("combat-tag.actions-blocked.commands.whitelist", true); }
    public List<String> commandList() { return plugin.getConfig().getStringList("combat-tag.actions-blocked.commands.list"); }

    // Combat Log
    public double logMoneyPenalty() { return plugin.getConfig().getDouble("combat-log-punishments.money-penalty", 0.0); }
    public boolean logKillOnLogout() { return plugin.getConfig().getBoolean("combat-log-punishments.kill-on-logout.enabled", true); }
    public boolean logDropInv() { return plugin.getConfig().getBoolean("combat-log-punishments.kill-on-logout.player-drops.inventory", true); }
    public boolean logDropExp() { return plugin.getConfig().getBoolean("combat-log-punishments.kill-on-logout.player-drops.experience", true); }
    public boolean logDropArmor() { return plugin.getConfig().getBoolean("combat-log-punishments.kill-on-logout.player-drops.armor", true); }
    public boolean logPunishOnKick() { return plugin.getConfig().getBoolean("combat-log-punishments.punish-on-kick.enabled", true); }
    public boolean logMatchKickReason() { return plugin.getConfig().getBoolean("combat-log-punishments.punish-on-kick.match-kick-reason", false); }
    public List<String> logKickReasons() { return plugin.getConfig().getStringList("combat-log-punishments.punish-on-kick.kick-reasons"); }
    public List<String> logCommands() { return plugin.getConfig().getStringList("combat-log-punishments.commands-on-combat-log"); }

    // PvP Toggle
    public boolean pvpDefault() { return plugin.getConfig().getBoolean("pvp-toggle.default-pvp", true); }
    public int pvpCooldown() { return plugin.getConfig().getInt("pvp-toggle.cooldown", 15); }
    public boolean pvpNametagsEnabled() { return plugin.getConfig().getBoolean("pvp-toggle.nametags.enabled", false); }
    public String pvpPrefixOn() { return plugin.getConfig().getString("pvp-toggle.nametags.prefix-on", "&4PvP On ⚔ "); }
    public String pvpPrefixOff() { return plugin.getConfig().getString("pvp-toggle.nametags.prefix-off", "&2PvP Off ⚔ "); }
    public double pvpDisabledFee() { return plugin.getConfig().getDouble("pvp-toggle.pvp-disabled-money-fee", 0.0); }
    public List<String> pvpCommandsOn() { return plugin.getConfig().getStringList("pvp-toggle.commands-pvp-on"); }
    public List<String> pvpCommandsOff() { return plugin.getConfig().getStringList("pvp-toggle.commands-pvp-off"); }
    public boolean pvpWGOverrides() { return plugin.getConfig().getBoolean("pvp-toggle.worldguard-overrides", true); }

    // Anti Border Hopping
    public boolean borderVulnerable() { return plugin.getConfig().getBoolean("anti-border-hopping.vulnerable.enabled", true); }
    public boolean borderRenewTag() { return plugin.getConfig().getBoolean("anti-border-hopping.vulnerable.renew-combat-tag", true); }
    public boolean borderPushBack() { return plugin.getConfig().getBoolean("anti-border-hopping.push-back.enabled", true); }
    public double borderPushForce() { return plugin.getConfig().getDouble("anti-border-hopping.push-back.force", 1.2); }
    public boolean borderRemoveElytra() { return plugin.getConfig().getBoolean("anti-border-hopping.push-back.remove-elytra", false); }

    // Item Cooldowns
    public int itemCooldown(String material, boolean global) {
        String path = global ? "item-cooldowns.global." : "item-cooldowns.combat.";
        return plugin.getConfig().getInt(path + material, -1);
    }

    // Player Kills
    public double killMoneyReward() { return plugin.getConfig().getDouble("player-kills.money-reward", 0.0); }
    public double killMoneyPenalty() { return plugin.getConfig().getDouble("player-kills.money-penalty", 0.0); }
    public boolean killMoneySteal() { return plugin.getConfig().getBoolean("player-kills.money-steal", false); }
    public double killExpSteal() { return plugin.getConfig().getDouble("player-kills.exp-steal", 0.0); }
    public int killCommandCooldown() { return plugin.getConfig().getInt("player-kills.commands-on-kill.cooldown", -1); }
    public List<String> killCommands() { return plugin.getConfig().getStringList("player-kills.commands-on-kill.commands"); }
    public List<String> respawnCommands() { return plugin.getConfig().getStringList("player-kills.commands-on-respawn"); }
    public List<String> killWGExclusions() { return plugin.getConfig().getStringList("player-kills.worldguard-exclusions"); }
    public boolean antiKillAbuseEnabled() { return plugin.getConfig().getBoolean("player-kills.anti-kill-abuse.enabled", true); }
    public int antiKillAbuseMax() { return plugin.getConfig().getInt("player-kills.anti-kill-abuse.max-kills", 5); }
    public int antiKillAbuseTime() { return plugin.getConfig().getInt("player-kills.anti-kill-abuse.time-limit", 20); }
    public boolean antiKillAbuseWarn() { return plugin.getConfig().getBoolean("player-kills.anti-kill-abuse.warn-before", true); }
    public List<String> antiKillAbuseCommands() { return plugin.getConfig().getStringList("player-kills.anti-kill-abuse.commands-on-abuse"); }
    public int respawnProtection() { return plugin.getConfig().getInt("player-kills.anti-kill-abuse.respawn-protection", 3); }

    // Other Settings
    public boolean pvpBlood() { return plugin.getConfig().getBoolean("other-combat-settings.pvp-blood", true); }
    public boolean ignoreNoDamageHits() { return plugin.getConfig().getBoolean("other-combat-settings.ignore-no-damage-hits", true); }
    public boolean protMsgsToActionBar() { return plugin.getConfig().getBoolean("other-combat-settings.protection-messages-to-action-bar", true); }
    public String playerDropMode() { return plugin.getConfig().getString("other-combat-settings.player-drop-mode", "ALWAYS"); }
    public boolean showHealthEnabled() { return plugin.getConfig().getBoolean("other-combat-settings.show-health-under-name.enabled", true); }
    public String showHealthName() { return plugin.getConfig().getString("other-combat-settings.show-health-under-name.display-name", "&c❤"); }

    // Disable On Hit
    public boolean disableFly() { return plugin.getConfig().getBoolean("disable-on-hit.fly", true); }
    public boolean restoreFly() { return plugin.getConfig().getBoolean("disable-on-hit.restore-fly", true); }
    public boolean disableGameMode() { return plugin.getConfig().getBoolean("disable-on-hit.gamemode", true); }
    public boolean disableDisguise() { return plugin.getConfig().getBoolean("disable-on-hit.disguise", true); }
    public boolean disableGodMode() { return plugin.getConfig().getBoolean("disable-on-hit.godmode", true); }
    public boolean disableElytra() { return plugin.getConfig().getBoolean("disable-on-hit.elytra", false); }
    public boolean disableInvisibility() { return plugin.getConfig().getBoolean("disable-on-hit.invisibility", false); }

    // Harmful Potions
    public List<String> harmfulPotions() { return plugin.getConfig().getStringList("harmful-potions"); }
}

