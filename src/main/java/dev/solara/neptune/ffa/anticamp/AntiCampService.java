package dev.solara.neptune.ffa.anticamp;

import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AntiCampService {
    private final JavaPlugin plugin;
    private final FfaSessionManager sessions;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Integer> stillChecks = new HashMap<>();
    private final Set<UUID> camping = new HashSet<>();
    private BukkitTask task;

    public AntiCampService(JavaPlugin plugin, FfaSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("anti-camp.enabled", true)) {
            return;
        }
        long period = Math.max(1, plugin.getConfig().getLong("anti-camp.check-seconds", 5)) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        camping.clear();
    }

    public boolean camping(Player player) {
        return camping.contains(player.getUniqueId());
    }

    public double rewardMultiplier(Player player) {
        return camping(player) ? plugin.getConfig().getDouble("anti-camp.reward-multiplier", 0.25D) : 1.0D;
    }

    private void tick() {
        double radius = plugin.getConfig().getDouble("anti-camp.radius", 4.0D);
        int max = plugin.getConfig().getInt("anti-camp.max-still-checks", 4);
        for (UUID uuid : sessions.activePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Location last = lastLocations.put(uuid, player.getLocation());
            if (last != null && last.getWorld().equals(player.getWorld()) && last.distance(player.getLocation()) <= radius) {
                int checks = stillChecks.merge(uuid, 1, Integer::sum);
                if (checks >= max) {
                    camping.add(uuid);
                    if (plugin.getConfig().getBoolean("anti-camp.glow", true)) {
                        player.setGlowing(true);
                    }
                    Text.send(player, plugin, "anti-camp");
                }
            } else {
                stillChecks.remove(uuid);
                camping.remove(uuid);
            }
        }
    }
}
