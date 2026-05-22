package dev.solara.neptune.ffa.quest;

import dev.solara.neptune.ffa.progression.ProgressionService;
import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.stats.FfaStatsService;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuestService {
    private final JavaPlugin plugin;
    private final FfaStatsService statsService;
    private final ProgressionService progressionService;
    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Map<UUID, Set<String>> completed = new HashMap<>();

    public QuestService(JavaPlugin plugin, FfaStatsService statsService, ProgressionService progressionService) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.progressionService = progressionService;
    }

    public void add(Player player, QuestType type, int amount) {
        if (!plugin.getConfig().getBoolean("quests.enabled", true)) {
            return;
        }
        updateGroup(player, "daily", type, amount);
        updateGroup(player, "weekly", type, amount);
    }

    public String summary(Player player) {
        StringBuilder builder = new StringBuilder();
        appendGroup(builder, player, "daily");
        appendGroup(builder, player, "weekly");
        return builder.isEmpty() ? "No quests configured." : builder.toString();
    }

    private void updateGroup(Player player, String group, QuestType type, int amount) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("quests." + group);
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            String path = "quests." + group + "." + id + ".";
            QuestType questType = parse(plugin.getConfig().getString(path + "type"));
            if (questType != type || isCompleted(player, group + ":" + id)) {
                continue;
            }

            int value = progress.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                    .merge(group + ":" + id, amount, Integer::sum);
            int target = plugin.getConfig().getInt(path + "target", 1);
            if (value >= target) {
                complete(player, group + ":" + id, path);
            }
        }
    }

    private void complete(Player player, String key, String path) {
        completed.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(key);
        int xp = plugin.getConfig().getInt(path + "xp", 0);
        int coins = plugin.getConfig().getInt(path + "coins", 0);
        FfaPlayerStats stats = statsService.get(player);
        stats.addCoins(coins);
        progressionService.addXp(player, stats, xp);
        Text.send(player, plugin, "quest-complete", "<quest>", key, "<xp>", String.valueOf(xp), "<coins>", String.valueOf(coins));
    }

    private boolean isCompleted(Player player, String key) {
        return completed.getOrDefault(player.getUniqueId(), Set.of()).contains(key);
    }

    private void appendGroup(StringBuilder builder, Player player, String group) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("quests." + group);
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            String key = group + ":" + id;
            int current = progress.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(key, 0);
            int target = plugin.getConfig().getInt("quests." + group + "." + id + ".target", 1);
            builder.append(group).append(" ").append(id).append(": ")
                    .append(Math.min(current, target)).append("/").append(target);
            if (isCompleted(player, key)) {
                builder.append(" complete");
            }
            builder.append("\n");
        }
    }

    private QuestType parse(String value) {
        try {
            return QuestType.valueOf(String.valueOf(value).toUpperCase());
        } catch (IllegalArgumentException exception) {
            return QuestType.KILLS;
        }
    }
}
