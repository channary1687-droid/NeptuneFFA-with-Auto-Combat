package dev.solara.neptune.ffa.combat;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public final class CombatTracker {
    private final JavaPlugin plugin;
    private final Map<UUID, Queue<Long>> clicks = new HashMap<>();
    private final Map<UUID, CombatState> states = new HashMap<>();

    public CombatTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void swing(Player player) {
        long now = System.currentTimeMillis();
        Queue<Long> timestamps = clicks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        timestamps.add(now);
        trim(timestamps, now);
        state(player).swings++;
    }

    public int hit(Player attacker, Player victim, double damage) {
        CombatState state = state(attacker);
        long now = System.currentTimeMillis();
        long comboReset = plugin.getConfig().getLong("combat.combo-reset-ms", 2500L);
        if (!victim.getUniqueId().equals(state.lastVictim) || now - state.lastHitAt > comboReset) {
            state.combo = 0;
        }
        state.combo++;
        state.lastHitAt = now;
        state.lastVictim = victim.getUniqueId();
        state.hits++;
        state.lastReach = attacker.getLocation().distance(victim.getLocation());
        state.damage += damage;
        return state.combo;
    }

    public void resetCombo(Player player) {
        state(player).combo = 0;
    }

    public CombatSnapshot snapshot(Player player) {
        CombatState state = state(player);
        Queue<Long> timestamps = clicks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        trim(timestamps, System.currentTimeMillis());
        double accuracy = state.swings == 0 ? 0.0D : Math.min(100.0D, (state.hits * 100.0D) / state.swings);
        return new CombatSnapshot(timestamps.size(), state.lastReach, accuracy, state.combo);
    }

    private CombatState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), ignored -> new CombatState());
    }

    private void trim(Queue<Long> timestamps, long now) {
        long window = plugin.getConfig().getLong("combat.cps-window-ms", 1000L);
        while (!timestamps.isEmpty() && now - timestamps.peek() > window) {
            timestamps.poll();
        }
    }

    private static final class CombatState {
        private UUID lastVictim;
        private long lastHitAt;
        private int combo;
        private int hits;
        private int swings;
        private double damage;
        private double lastReach;
    }
}
