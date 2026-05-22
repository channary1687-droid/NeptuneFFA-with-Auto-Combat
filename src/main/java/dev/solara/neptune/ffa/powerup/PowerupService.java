package dev.solara.neptune.ffa.powerup;

import dev.solara.neptune.ffa.arena.FfaArena;
import dev.solara.neptune.ffa.arena.FfaArenaService;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PowerupService implements Listener {
    private final JavaPlugin plugin;
    private final FfaArenaService arenas;
    private final FfaSessionManager sessions;
    private final Map<UUID, PowerupType> powerups = new HashMap<>();
    private BukkitTask task;

    public PowerupService(JavaPlugin plugin, FfaArenaService arenas, FfaSessionManager sessions) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.sessions = sessions;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("powerups.enabled", true)) {
            return;
        }
        long interval = Math.max(10, plugin.getConfig().getLong("powerups.interval-seconds", 90)) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnAll, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        powerups.clear();
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !sessions.isActive(player)) {
            return;
        }
        PowerupType type = powerups.remove(event.getItem().getUniqueId());
        if (type == null) {
            return;
        }

        event.setCancelled(true);
        event.getItem().remove();
        int duration = plugin.getConfig().getInt("powerups.types." + type.name() + ".duration-seconds", 10);
        int amplifier = plugin.getConfig().getInt("powerups.types." + type.name() + ".amplifier", 0);
        player.addPotionEffect(new PotionEffect(type.effect(), duration * 20, amplifier));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.4F);
        Text.send(player, plugin, "powerup", "<powerup>", type.name());
    }

    private void spawnAll() {
        List<PowerupType> types = List.of(PowerupType.values());
        int perArena = plugin.getConfig().getInt("powerups.per-arena", 2);
        for (FfaArena arena : arenas.all()) {
            if (!arena.isSetup()) {
                continue;
            }
            for (int i = 0; i < perArena; i++) {
                PowerupType type = types.get(ThreadLocalRandom.current().nextInt(types.size()));
                spawn(arena, type);
            }
        }
    }

    private void spawn(FfaArena arena, PowerupType type) {
        Location location = sessions.randomLocation(arena);
        Material material = Material.matchMaterial(plugin.getConfig().getString("powerups.types." + type.name() + ".material", "SUGAR"));
        ItemStack itemStack = new ItemStack(material == null ? Material.SUGAR : material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color("&b" + type.name().toLowerCase(Locale.ROOT) + " powerup"));
            itemStack.setItemMeta(meta);
        }
        Item item = location.getWorld().dropItemNaturally(location, itemStack);
        item.setGlowing(true);
        item.setPickupDelay(20);
        powerups.put(item.getUniqueId(), type);
        location.getWorld().spawnParticle(Particle.END_ROD, location, 30, 0.4D, 0.5D, 0.4D);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            powerups.remove(item.getUniqueId());
            item.remove();
        }, plugin.getConfig().getLong("powerups.despawn-seconds", 45) * 20L);
    }
}
