package dev.solara.neptune.ffa.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AntiKillAbuseService {
    private final JavaPlugin plugin;
    private final CombatConfig config;
    private final Map<UUID, Map<UUID, List<Long>>> killHistory = new HashMap<>();

    public AntiKillAbuseService(JavaPlugin plugin, CombatConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean check(Player killer, Player victim) {
        if (!config.antiKillAbuseEnabled()) return false;

        UUID kId = killer.getUniqueId();
        UUID vId = victim.getUniqueId();

        Map<UUID, List<Long>> victimMap = killHistory.computeIfAbsent(kId, k -> new HashMap<>());
        List<Long> times = victimMap.computeIfAbsent(vId, k -> new ArrayList<>());

        long now = System.currentTimeMillis();
        long window = config.antiKillAbuseTime() * 1000L;
        times.removeIf(t -> now - t > window);
        times.add(now);

        if (times.size() >= config.antiKillAbuseMax()) {
            punish(killer, victim);
            return true;
        }
        
        if (config.antiKillAbuseWarn() && times.size() == config.antiKillAbuseMax() - 1) {
            // warn killer
        }

        return false;
    }

    private void punish(Player killer, Player victim) {
        for (String cmd : config.antiKillAbuseCommands()) {
            String processed = cmd.replace("{player}", killer.getName()).replace("{victim}", victim.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }
    }
}
