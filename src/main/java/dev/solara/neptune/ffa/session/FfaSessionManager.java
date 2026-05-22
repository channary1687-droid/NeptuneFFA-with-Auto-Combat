package dev.solara.neptune.ffa.session;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.kit.IKit;
import dev.solara.neptune.ffa.arena.FfaArena;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class FfaSessionManager {
    private final JavaPlugin plugin;
    private final NeptuneAPI neptune;
    private final Map<UUID, ActiveFfaPlayer> active = new HashMap<>();

    public FfaSessionManager(JavaPlugin plugin, NeptuneAPI neptune) {
        this.plugin = plugin;
        this.neptune = neptune;
    }

    public void join(Player player, FfaArena arena, IKit kit) {
        if (!arena.isSetup()) {
            Text.send(player, plugin, "arena-not-setup");
            return;
        }

        active.put(player.getUniqueId(), new ActiveFfaPlayer(arena, kit));
        neptune.getProfileService().getProfile(player.getUniqueId()).thenAccept(profile ->
                Bukkit.getScheduler().runTask(plugin, () -> profile.setState("FFA")));

        resetPlayer(player, kit);
        player.teleport(spawnLocation(arena));
        Text.send(player, plugin, "joined-persistent", "<arena>", arena.name(), "<kit>", kit.getName());
    }

    public void leave(Player player) {
        ActiveFfaPlayer removed = active.remove(player.getUniqueId());
        if (removed == null) {
            Text.send(player, plugin, "not-queued");
            return;
        }

        resetBase(player);
        neptune.getProfileService().getProfile(player.getUniqueId()).thenAccept(profile ->
                Bukkit.getScheduler().runTask(plugin, profile::toLobby));
        Text.send(player, plugin, "left-persistent");
    }

    public void leaveAfterDeath(Player player) {
        ActiveFfaPlayer removed = active.remove(player.getUniqueId());
        if (removed == null) {
            return;
        }

        resetBase(player);
        neptune.getProfileService().getProfile(player.getUniqueId()).thenAccept(profile ->
                Bukkit.getScheduler().runTask(plugin, profile::toLobby));
    }

    public void removeSilently(UUID uuid) {
        active.remove(uuid);
    }

    public ActiveFfaPlayer active(Player player) {
        return active.get(player.getUniqueId());
    }

    public boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public Collection<UUID> activePlayers() {
        return Collections.unmodifiableSet(active.keySet());
    }

    public void refreshAfterKill(Player player) {
        ActiveFfaPlayer ffa = active(player);
        if (ffa == null) {
            return;
        }
        resetPlayer(player, ffa.kit());
        Text.send(player, plugin, "kill-reset");
    }

    public Location respawnLocation(Player player) {
        ActiveFfaPlayer ffa = active(player);
        if (ffa == null) {
            return null;
        }
        return spawnLocation(ffa.arena());
    }

    public void resetActivePlayer(Player player) {
        ActiveFfaPlayer ffa = active(player);
        if (ffa != null) {
            resetPlayer(player, ffa.kit());
        }
    }

    public void switchArena(Player player, FfaArena arena) {
        ActiveFfaPlayer ffa = active(player);
        if (ffa == null || !arena.isSetup()) {
            return;
        }
        active.put(player.getUniqueId(), new ActiveFfaPlayer(arena, ffa.kit()));
        player.teleport(spawnLocation(arena));
        resetPlayer(player, ffa.kit());
    }

    public Location randomLocation(FfaArena arena) {
        return randomSafeLocation(arena);
    }

    private Location spawnLocation(FfaArena arena) {
        Location spawn = arena.spawn();
        if (spawn != null && spawn.getWorld() != null && spawn.getWorld().equals(arena.pos1().getWorld())) {
            return spawn.clone();
        }
        return randomSafeLocation(arena);
    }

    public int playing(FfaArena arena) {
        int count = 0;
        for (ActiveFfaPlayer ffa : active.values()) {
            if (ffa.arena().name().equalsIgnoreCase(arena.name())) {
                count++;
            }
        }
        return count;
    }

    public void resetPlayer(Player player, IKit kit) {
        resetBase(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Math.max(1.0D, kit.getHealth()));
        player.setHealth(Math.max(1.0D, kit.getHealth()));
        kit.giveLoadout(player.getUniqueId());
        for (PotionEffect effect : kit.getPotionEffects()) {
            player.addPotionEffect(effect);
        }
        repairInventory(player.getInventory().getContents());
        repairInventory(player.getInventory().getArmorContents());
    }

    private void resetBase(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setArrowsInBody(0);
        player.setAbsorptionAmount(0.0D);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void repairInventory(ItemStack[] contents) {
        for (ItemStack item : contents) {
            if (item == null) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(0);
                item.setItemMeta(meta);
            }
        }
    }

    private Location randomSafeLocation(FfaArena arena) {
        Location pos1 = arena.pos1();
        Location pos2 = arena.pos2();
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 80; attempt++) {
            int x = random.nextInt(minX, maxX + 1);
            int z = random.nextInt(minZ, maxZ + 1);
            Location safe = highestSafeLocation(world, x, z, minY, maxY);
            if (safe != null) {
                return safe;
            }
        }
        return pos1.clone().add(0.5D, 0.0D, 0.5D);
    }

    private Location highestSafeLocation(World world, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            Block floor = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (floor.getType().isSolid() && isPassable(feet.getType()) && isPassable(head.getType())) {
                return new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
            }
        }
        return null;
    }

    private boolean isPassable(Material material) {
        return material.isAir() || !material.isSolid();
    }
}
