package dev.solara.neptune.ffa.combat;

import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatTagService {
    private final JavaPlugin plugin;
    private final CombatConfig config;
    private final Map<UUID, TagData> tags = new HashMap<>();

    public CombatTagService(JavaPlugin plugin, CombatConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void tag(Player player) {
        if (!config.tagEnabled()) return;

        UUID uuid = player.getUniqueId();
        TagData existing = tags.remove(uuid);
        if (existing != null) {
            existing.cleanup();
        }

        long seconds = config.tagTime();
        TagData data = new TagData(player, seconds);
        tags.put(uuid, data);

        if (config.tagGlowing()) {
            player.setGlowing(true);
        }

        if (config.tagCloseInventory()) {
            player.closeInventory();
        }
    }

    public boolean isTagged(Player player) {
        return tags.containsKey(player.getUniqueId());
    }

    public boolean isTagged(UUID uuid) {
        return tags.containsKey(uuid);
    }

    public void untag(UUID uuid) {
        TagData data = tags.remove(uuid);
        if (data != null) {
            data.cleanup();
        }
    }

    public void shutdown() {
        for (TagData data : tags.values()) {
            data.cleanup();
        }
        tags.clear();
    }

    private final class TagData {
        private final UUID uuid;
        private long remainingTicks;
        private final BukkitTask task;
        private BossBar bossBar;

        public TagData(Player player, long seconds) {
            this.uuid = player.getUniqueId();
            this.remainingTicks = seconds * 20L;

            if (config.bossBarEnabled()) {
                this.bossBar = Bukkit.createBossBar(
                        "", // Title updated below
                        config.bossBarColor(),
                        config.bossBarStyle()
                );
                this.bossBar.addPlayer(player);
                this.bossBar.setProgress(1.0);
                updateDisplays(player);
            }

            this.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                remainingTicks -= 2L;
                if (remainingTicks <= 0) {
                    cleanup();
                    tags.remove(uuid);
                    Player online = Bukkit.getPlayer(uuid);
                    if (online != null) {
                        Text.send(online, plugin, "combat-tag-expired");
                    }
                    return;
                }

                Player online = Bukkit.getPlayer(uuid);
                if (online == null) return;

                updateDisplays(online);
            }, 0L, 2L);
        }

        private void updateDisplays(Player player) {
            double secondsLeft = remainingTicks / 20.0;
            String timeStr = String.format("%.1f", secondsLeft);

            if (bossBar != null) {
                // BossBar.setTitle only accepts String, so we must use legacy conversion or just keep tags if it supports it
                // Modern servers often support MiniMessage in these fields if handled by a proxy or plugin,
                // but for vanilla Bukkit BossBar we'll use Text.parse and convert to legacy string.
                bossBar.setTitle(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().serialize(Text.parse(config.bossBarMessage().replace("<time>", timeStr)))));
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) remainingTicks / (config.tagTime() * 20.0))));
            }

            if (config.actionBarEnabled()) {
                String msg = config.actionBarMessage()
                        .replace("<time>", timeStr)
                        .replace("<barsLeft>", generateBars(true))
                        .replace("<barsPassed>", generateBars(false));
                Text.sendActionBar(player, msg);
            }
        }

        private String generateBars(boolean left) {
            int total = config.actionBarTotalBars();
            double progress = (double) remainingTicks / (config.tagTime() * 20.0);
            int count = (int) (total * progress);
            if (!left) count = total - count;
            
            StringBuilder sb = new StringBuilder();
            String symbol = config.actionBarSymbol();
            for (int i = 0; i < count; i++) {
                sb.append(symbol);
            }
            return sb.toString();
        }

        public void cleanup() {
            task.cancel();
            if (bossBar != null) {
                bossBar.removeAll();
            }
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                if (config.tagGlowing()) {
                    online.setGlowing(false);
                }
            }
        }
    }
}

