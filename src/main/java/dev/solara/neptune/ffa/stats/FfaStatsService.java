package dev.solara.neptune.ffa.stats;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class FfaStatsService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, FfaPlayerStats> stats = new LinkedHashMap<>();

    public FfaStatsService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        stats.clear();
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("players")) {
            return;
        }

        for (String id : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(id);
            String path = "players." + id + ".";
            FfaPlayerStats playerStats = new FfaPlayerStats(uuid, config.getString(path + "name", "Unknown"));
            playerStats.level(config.getInt(path + "level", 1));
            playerStats.prestige(config.getInt(path + "prestige", 0));
            playerStats.xp(config.getInt(path + "xp", 0));
            playerStats.addCoins(config.getInt(path + "coins", 0));
            playerStats.kills(config.getInt(path + "kills", 0));
            playerStats.deaths(config.getInt(path + "deaths", 0));
            playerStats.bestStreak(config.getInt(path + "best-streak", 0));
            playerStats.damageDealt(config.getDouble(path + "damage-dealt", 0.0D));
            playerStats.hits(config.getInt(path + "hits", 0));
            playerStats.swings(config.getInt(path + "swings", 0));
            playerStats.headshots(config.getInt(path + "headshots", 0));
            playerStats.criticalHits(config.getInt(path + "critical-hits", 0));
            playerStats.bountyClaims(config.getInt(path + "bounty-claims", 0));
            playerStats.eventsWon(config.getInt(path + "events-won", 0));
            playerStats.battlepassXp(config.getInt(path + "battlepass-xp", 0));
            playerStats.battlepassTier(config.getInt(path + "battlepass-tier", 0));
            playerStats.seasonPoints(config.getInt(path + "season-points", 0));
            stats.put(uuid, playerStats);
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (FfaPlayerStats playerStats : stats.values()) {
            String path = "players." + playerStats.uuid() + ".";
            config.set(path + "name", playerStats.name());
            config.set(path + "kills", playerStats.kills());
            config.set(path + "deaths", playerStats.deaths());
            config.set(path + "best-streak", playerStats.bestStreak());
            config.set(path + "coins", playerStats.coins());
            config.set(path + "xp", playerStats.xp());
            config.set(path + "level", playerStats.level());
            config.set(path + "prestige", playerStats.prestige());
            config.set(path + "damage-dealt", playerStats.damageDealt());
            config.set(path + "hits", playerStats.hits());
            config.set(path + "swings", playerStats.swings());
            config.set(path + "headshots", playerStats.headshots());
            config.set(path + "critical-hits", playerStats.criticalHits());
            config.set(path + "bounty-claims", playerStats.bountyClaims());
            config.set(path + "events-won", playerStats.eventsWon());
            config.set(path + "battlepass-xp", playerStats.battlepassXp());
            config.set(path + "battlepass-tier", playerStats.battlepassTier());
            config.set(path + "season-points", playerStats.seasonPoints());
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save players.yml");
            exception.printStackTrace();
        }
    }

    public FfaPlayerStats get(Player player) {
        FfaPlayerStats playerStats = stats.computeIfAbsent(player.getUniqueId(),
                uuid -> new FfaPlayerStats(uuid, player.getName()));
        playerStats.name(player.getName());
        return playerStats;
    }

    public Collection<FfaPlayerStats> all() {
        return stats.values();
    }
}
