package dev.solara.neptune.ffa.hotzone;

import dev.solara.neptune.ffa.arena.FfaArena;
import dev.solara.neptune.ffa.session.ActiveFfaPlayer;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class HotzoneService {
    private final JavaPlugin plugin;
    private final FfaSessionManager sessions;
    private BukkitTask task;

    public HotzoneService(JavaPlugin plugin, FfaSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("hotzone.enabled", true)) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGlowing(false);
        }
    }

    public boolean inside(Player player) {
        ActiveFfaPlayer active = sessions.active(player);
        if (active == null) {
            return false;
        }

        Location center = center(active.arena());
        return center != null && center.getWorld() != null && center.getWorld().equals(player.getWorld())
                && center.distance(player.getLocation()) <= plugin.getConfig().getDouble("hotzone.radius", 8.0D);
    }

    public double xpMultiplier(Player player) {
        return inside(player) ? plugin.getConfig().getDouble("hotzone.xp-multiplier", 2.0D) : 1.0D;
    }

    public double coinsMultiplier(Player player) {
        return inside(player) ? plugin.getConfig().getDouble("hotzone.coins-multiplier", 2.0D) : 1.0D;
    }

    public double killMultiplier(Player player) {
        return inside(player) ? plugin.getConfig().getDouble("hotzone.kill-multiplier", 2.0D) : 1.0D;
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("hotzone.glow", true)) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessions.isActive(player)) {
                player.setGlowing(inside(player));
            }
        }
    }

    private Location center(FfaArena arena) {
        if (!arena.isSetup()) {
            return null;
        }
        Location pos1 = arena.pos1();
        Location pos2 = arena.pos2();
        return new Location(
                pos1.getWorld(),
                (pos1.getX() + pos2.getX()) / 2.0D,
                (pos1.getY() + pos2.getY()) / 2.0D,
                (pos1.getZ() + pos2.getZ()) / 2.0D
        );
    }
}
