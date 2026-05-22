package dev.solara.neptune.ffa.rotation;

import dev.solara.neptune.ffa.arena.FfaArena;
import dev.solara.neptune.ffa.arena.FfaArenaService;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MapRotationService {
    private final JavaPlugin plugin;
    private final FfaArenaService arenas;
    private final FfaSessionManager sessions;
    private int index;
    private BukkitTask task;

    public MapRotationService(JavaPlugin plugin, FfaArenaService arenas, FfaSessionManager sessions) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.sessions = sessions;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("map-rotation.enabled", true)) {
            return;
        }
        long interval = Math.max(1, plugin.getConfig().getLong("map-rotation.interval-minutes", 30)) * 60L * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::rotate, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public void rotate() {
        List<FfaArena> ready = arenas.all().stream().filter(FfaArena::isSetup).toList();
        if (ready.isEmpty()) {
            return;
        }
        index = (index + 1) % ready.size();
        FfaArena next = ready.get(index);
        int warning = plugin.getConfig().getInt("map-rotation.warning-seconds", 30);
        broadcast("rotation-warning", "<seconds>", String.valueOf(warning));
        Bukkit.getScheduler().runTaskLater(plugin, () -> movePlayers(next), Math.max(0, warning) * 20L);
    }

    private void movePlayers(FfaArena arena) {
        for (UUID uuid : new ArrayList<>(sessions.activePlayers())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                sessions.switchArena(player, arena);
            }
        }
        broadcast("rotation-complete", "<arena>", arena.name());
    }

    private void broadcast(String key, String... placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessions.isActive(player)) {
                Text.send(player, plugin, key, placeholders);
            }
        }
    }
}
