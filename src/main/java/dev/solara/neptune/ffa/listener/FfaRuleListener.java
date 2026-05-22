package dev.solara.neptune.ffa.listener;

import dev.solara.neptune.ffa.arena.FfaArena;
import dev.solara.neptune.ffa.arena.FfaArenaService;
import dev.solara.neptune.ffa.session.ActiveFfaPlayer;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.util.KitRules;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FfaRuleListener implements Listener {
    private final JavaPlugin plugin;
    private final FfaSessionManager sessions;
    private final FfaArenaService arenas;
    private final Set<Location> placedBlocks = new HashSet<>();
    private final Map<String, Map<Location, BlockData>> changedBlocks = new HashMap<>();
    private BukkitTask autoRegenTask;

    public FfaRuleListener(JavaPlugin plugin, FfaSessionManager sessions, FfaArenaService arenas) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.arenas = arenas;
    }

    public void start() {
        long interval = Math.max(1, plugin.getConfig().getLong("arena-auto-regen-minutes", 5)) * 60L * 20L;
        autoRegenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::restoreAutoRegenArenas, interval, interval);
    }

    public void stop() {
        if (autoRegenTask != null) {
            autoRegenTask.cancel();
            autoRegenTask = null;
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ActiveFfaPlayer ffa = sessions.active(event.getPlayer());
        if (ffa == null) {
            return;
        }

        if (!ffa.arena().allowPlace() || !KitRules.enabled(ffa.kit(), "build")) {
            event.setCancelled(true);
            return;
        }

        Location location = event.getBlockPlaced().getLocation();
        rememberOriginal(ffa, location, event.getBlockReplacedState().getBlockData());
        placedBlocks.add(location);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ActiveFfaPlayer ffa = sessions.active(event.getPlayer());
        if (ffa == null) {
            return;
        }

        Location location = event.getBlock().getLocation();
        if (!ffa.arena().allowBreak() || !KitRules.enabled(ffa.kit(), "build")) {
            event.setCancelled(true);
            return;
        }
        if (!KitRules.enabled(ffa.kit(), "arenaBreak") && !placedBlocks.contains(location)) {
            event.setCancelled(true);
            return;
        }

        rememberOriginal(ffa, location, event.getBlock().getBlockData());
        placedBlocks.remove(location);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        ActiveFfaPlayer ffa = sessions.active(event.getEntity());
        if (ffa == null || !KitRules.enabled(ffa.kit(), "resetArenaAfterMatch")) {
            return;
        }

        restoreArena(ffa.arena());
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ActiveFfaPlayer ffa = sessions.active(player);
        if (ffa == null || KitRules.enabled(ffa.kit(), "hunger")) {
            return;
        }

        event.setCancelled(true);
        player.setFoodLevel(20);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ActiveFfaPlayer ffa = sessions.active(player);
        if (ffa == null) {
            return;
        }

        if (!KitRules.enabled(ffa.kit(), "damage")) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !KitRules.enabled(ffa.kit(), "fallDamage")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ActiveFfaPlayer ffa = sessions.active(player);
        if (ffa == null) {
            return;
        }

        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                && !KitRules.enabled(ffa.kit(), "saturationHeal")) {
            event.setCancelled(true);
        }
    }

    private void rememberOriginal(ActiveFfaPlayer ffa, Location location, BlockData blockData) {
        changedBlocks
                .computeIfAbsent(ffa.arena().name().toLowerCase(), ignored -> new HashMap<>())
                .putIfAbsent(blockLocation(location), blockData.clone());
    }

    private void restoreAutoRegenArenas() {
        for (FfaArena arena : arenas.all()) {
            if (arena.autoRegen()) {
                restoreArena(arena);
            }
        }
    }

    private void restoreArena(FfaArena arena) {
        Map<Location, BlockData> changes = changedBlocks.remove(arena.name().toLowerCase());
        if (changes == null || changes.isEmpty()) {
            return;
        }

        for (Map.Entry<Location, BlockData> entry : changes.entrySet()) {
            Location location = entry.getKey();
            if (location.getWorld() != null) {
                location.getBlock().setBlockData(entry.getValue(), false);
            }
            placedBlocks.remove(location);
        }
    }

    private Location blockLocation(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
