package dev.solara.neptune.ffa.combat;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PvPToggleService {
    private final JavaPlugin plugin;
    private final CombatConfig config;
    private final Map<UUID, Boolean> pvpEnabled = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PvPToggleService(JavaPlugin plugin, CombatConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean isPvPEnabled(Player player) {
        return pvpEnabled.getOrDefault(player.getUniqueId(), config.pvpDefault());
    }

    public void toggle(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = config.pvpCooldown() * 1000L;

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < cooldown) {
            // handle message
            return;
        }

        boolean current = isPvPEnabled(player);
        pvpEnabled.put(uuid, !current);
        cooldowns.put(uuid, now);

        // handle commands/nametags
    }
}
