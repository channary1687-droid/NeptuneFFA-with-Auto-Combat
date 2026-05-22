package dev.solara.neptune.ffa.event;

import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class FfaEventService {
    private final JavaPlugin plugin;
    private final FfaSessionManager sessions;
    private BukkitTask task;
    private BukkitTask endTask;
    private FfaEventType active;

    public FfaEventService(JavaPlugin plugin, FfaSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("events.enabled", true)) {
            return;
        }
        long interval = Math.max(20L, plugin.getConfig().getLong("events.interval-seconds", 600L) * 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::startRandomEvent, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
        active = null;
    }

    public FfaEventType active() {
        return active;
    }

    public boolean active(FfaEventType type) {
        return active == type;
    }

    public boolean force(FfaEventType type) {
        if (active != null) {
            endEvent();
        }
        active = type;
        broadcast("event-start", "<event>", active.name().replace('_', ' '));
        applyStartEffects();
        long duration = Math.max(10L, plugin.getConfig().getLong("events.duration-seconds", 120L)) * 20L;
        endTask = Bukkit.getScheduler().runTaskLater(plugin, this::endEvent, duration);
        return true;
    }

    public void endActive() {
        if (active != null) {
            endEvent();
        }
    }

    private void startRandomEvent() {
        if (active != null) {
            return;
        }

        List<FfaEventType> pool = pool();
        if (pool.isEmpty()) {
            return;
        }
        active = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        broadcast("event-start", "<event>", active.name().replace('_', ' '));
        applyStartEffects();

        long duration = Math.max(10L, plugin.getConfig().getLong("events.duration-seconds", 120L)) * 20L;
        endTask = Bukkit.getScheduler().runTaskLater(plugin, this::endEvent, duration);
    }

    private void endEvent() {
        active = null;
        broadcast("event-end");
    }

    private List<FfaEventType> pool() {
        List<FfaEventType> result = new ArrayList<>();
        for (String name : plugin.getConfig().getStringList("events.pool")) {
            try {
                result.add(FfaEventType.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private void applyStartEffects() {
        if (active == FfaEventType.SPEED_FFA) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (sessions.isActive(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 120, 1));
                }
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessions.isActive(player)) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
            }
        }
    }

    private void broadcast(String key, String... placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessions.isActive(player)) {
                Text.send(player, plugin, key, placeholders);
            }
        }
    }
}
