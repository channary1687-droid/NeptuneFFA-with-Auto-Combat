package dev.solara.neptune.ffa.bounty;

import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.stats.FfaStatsService;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BountyService {
    private final JavaPlugin plugin;
    private final FfaStatsService statsService;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public BountyService(JavaPlugin plugin, FfaStatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
    }

    public boolean isBounty(Player player) {
        return bars.containsKey(player.getUniqueId());
    }

    public void mark(Player target) {
        if (!plugin.getConfig().getBoolean("bounty.enabled", true) || isBounty(target)) {
            return;
        }

        if (plugin.getConfig().getBoolean("bounty.glow-target", true)) {
            target.setGlowing(true);
        }
        BossBar bar = Bukkit.createBossBar("BOUNTY TARGET: " + target.getName(), BarColor.RED, BarStyle.SEGMENTED_10);
        if (plugin.getConfig().getBoolean("bounty.bossbar", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                bar.addPlayer(online);
            }
        }
        bars.put(target.getUniqueId(), bar);
        giveTrackers(target);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8F, 1.2F);
        broadcast("bounty-start", "<player>", target.getName());
    }

    public void claim(Player killer, Player target) {
        BossBar bar = bars.remove(target.getUniqueId());
        if (bar == null) {
            return;
        }
        bar.removeAll();
        target.setGlowing(false);

        int coins = plugin.getConfig().getInt("bounty.reward-coins", 250);
        FfaPlayerStats stats = statsService.get(killer);
        stats.addCoins(coins);
        stats.addBountyClaim();
        broadcast("bounty-claim", "<killer>", killer.getName(), "<target>", target.getName(), "<coins>", String.valueOf(coins));
    }

    public void shutdown() {
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
        bars.clear();
    }

    private void giveTrackers(Player target) {
        if (!plugin.getConfig().getBoolean("bounty.tracker-compass", true)) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(target)) {
                continue;
            }
            ItemStack compass = new ItemStack(Material.COMPASS);
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Bounty Tracker: " + target.getName());
                meta.setLodestone(target.getLocation());
                meta.setLodestoneTracked(false);
                compass.setItemMeta(meta);
            }
            player.getInventory().addItem(compass);
        }
    }

    private void broadcast(String key, String... placeholders) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            Text.send(online, plugin, key, placeholders);
        }
    }
}
