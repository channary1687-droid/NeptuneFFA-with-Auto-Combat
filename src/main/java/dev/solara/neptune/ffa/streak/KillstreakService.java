package dev.solara.neptune.ffa.streak;

import dev.solara.neptune.ffa.bounty.BountyService;
import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class KillstreakService {
    private final JavaPlugin plugin;
    private final BountyService bountyService;

    public KillstreakService(JavaPlugin plugin, BountyService bountyService) {
        this.plugin = plugin;
        this.bountyService = bountyService;
    }

    public void handle(Player player, FfaPlayerStats stats) {
        String path = "killstreaks." + stats.currentStreak() + ".";
        if (!plugin.getConfig().contains(path)) {
            return;
        }

        int speedSeconds = plugin.getConfig().getInt(path + "speed-seconds", 0);
        if (speedSeconds > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedSeconds * 20, 0));
        }

        int coins = plugin.getConfig().getInt(path + "coins", 0);
        if (coins > 0) {
            stats.addCoins(coins);
        }

        if (plugin.getConfig().getBoolean(path + "broadcast", false)) {
            broadcast("streak-broadcast", "<player>", player.getName(), "<streak>", String.valueOf(stats.currentStreak()));
        }

        if (plugin.getConfig().getBoolean(path + "bounty", false)) {
            bountyService.mark(player);
        }

        if (plugin.getConfig().getBoolean(path + "supply-drop", false)) {
            player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 80, 1.0D, 1.0D, 1.0D);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
            broadcast("supply-drop", "<player>", player.getName());
        }

        if (plugin.getConfig().getBoolean(path + "champion", false)) {
            player.getWorld().strikeLightningEffect(player.getLocation());
            broadcast("champion", "<player>", player.getName());
        }
    }

    private void broadcast(String key, String... placeholders) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            Text.send(online, plugin, key, placeholders);
        }
    }
}
