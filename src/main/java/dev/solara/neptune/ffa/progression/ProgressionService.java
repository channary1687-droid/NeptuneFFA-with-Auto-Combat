package dev.solara.neptune.ffa.progression;

import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProgressionService {
    private final JavaPlugin plugin;

    public ProgressionService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void addXp(Player player, FfaPlayerStats stats, int amount) {
        stats.xp(stats.xp() + Math.max(0, amount));
        boolean leveled = false;
        while (stats.xp() >= xpRequired(stats.level())) {
            stats.xp(stats.xp() - xpRequired(stats.level()));
            stats.level(stats.level() + 1);
            leveled = true;
        }
        if (leveled) {
            Text.send(player, plugin, "level-up", "<level>", String.valueOf(stats.level()));
        }

        int maxPrestige = plugin.getConfig().getInt("progression.prestige.max", 3);
        int required = plugin.getConfig().getInt("progression.prestige.level-required", 100);
        if (stats.level() >= required && stats.prestige() < maxPrestige) {
            stats.prestige(stats.prestige() + 1);
            stats.level(1);
            stats.xp(0);
            Text.send(player, plugin, "prestige-up", "<prestige>", roman(stats.prestige()));
        }
    }

    private int xpRequired(int level) {
        int base = plugin.getConfig().getInt("progression.levels.base-xp", 100);
        double multiplier = plugin.getConfig().getDouble("progression.levels.multiplier", 1.35D);
        return Math.max(1, (int) Math.round(base * Math.pow(multiplier, Math.max(0, level - 1))));
    }

    private String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(value);
        };
    }
}
