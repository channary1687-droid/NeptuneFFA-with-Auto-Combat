package dev.solara.neptune.ffa.battlepass;

import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BattlepassService {
    private final JavaPlugin plugin;

    public BattlepassService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void addXp(Player player, FfaPlayerStats stats, int amount) {
        if (!plugin.getConfig().getBoolean("battlepass.enabled", true)) {
            return;
        }
        stats.addBattlepassXp(amount);
        int tierXp = Math.max(1, plugin.getConfig().getInt("battlepass.tier-xp", 250));
        int maxTier = plugin.getConfig().getInt("battlepass.max-tier", 50);
        while (stats.battlepassXp() >= tierXp && stats.battlepassTier() < maxTier) {
            stats.battlepassXp(stats.battlepassXp() - tierXp);
            stats.battlepassTier(stats.battlepassTier() + 1);
            reward(player, stats, stats.battlepassTier());
        }
    }

    public String status(FfaPlayerStats stats) {
        int tierXp = Math.max(1, plugin.getConfig().getInt("battlepass.tier-xp", 250));
        return "Tier " + stats.battlepassTier() + "/" + plugin.getConfig().getInt("battlepass.max-tier", 50)
                + " XP " + stats.battlepassXp() + "/" + tierXp;
    }

    private void reward(Player player, FfaPlayerStats stats, int tier) {
        String path = "battlepass.rewards." + tier + ".";
        int coins = plugin.getConfig().getInt(path + "coins", 0);
        stats.addCoins(coins);
        Text.send(player, plugin, "battlepass-tier", "<tier>", String.valueOf(tier));
    }
}
