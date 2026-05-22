package dev.solara.neptune.ffa.listener;

import dev.lrxh.api.events.MatchStartEvent;
import dev.lrxh.api.match.IFffaFightMatch;
import dev.lrxh.api.match.participant.IParticipant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class FfaSpreadListener implements Listener {
    private final JavaPlugin plugin;

    public FfaSpreadListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMatchStart(MatchStartEvent event) {
        if (!plugin.getConfig().getBoolean("spread-on-start", true)) {
            return;
        }
        if (!(event.getMatch() instanceof IFffaFightMatch match)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> spread(match), 1L);
    }

    private void spread(IFffaFightMatch match) {
        List<IParticipant> participants = match.getParticipants();
        for (int i = 0; i < participants.size(); i++) {
            IParticipant participant = participants.get(i);
            Player player = Bukkit.getPlayer(participant.getPlayerUUID());
            if (player == null || !player.isOnline()) {
                continue;
            }

            Location location = randomSafeLocation(match, i, participants.size());
            player.teleport(location);
        }
    }

    private Location randomSafeLocation(IFffaFightMatch match, int index, int total) {
        Location min = match.getArena().getMin();
        Location max = match.getArena().getMax();
        if (min == null || max == null || min.getWorld() == null) {
            return fallbackLocation(match, index, total);
        }

        World world = min.getWorld();
        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 80; attempt++) {
            int x = random.nextInt(minX, maxX + 1);
            int z = random.nextInt(minZ, maxZ + 1);
            Location safe = highestSafeLocation(world, x, z, minY, maxY);
            if (safe != null) {
                return safe;
            }
        }

        return fallbackLocation(match, index, total);
    }

    private Location highestSafeLocation(World world, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            Block floor = world.getBlockAt(x, y, z);
            if (floor.getType().isSolid() && isPassable(feet.getType()) && isPassable(head.getType())) {
                return new Location(world, x + 0.5, y + 1.0, z + 0.5);
            }
        }
        return null;
    }

    private boolean isPassable(Material material) {
        return material.isAir() || !material.isSolid();
    }

    private Location fallbackLocation(IFffaFightMatch match, int index, int total) {
        Location red = match.getArena().getRedSpawn();
        Location blue = match.getArena().getBlueSpawn();
        if (red == null) {
            return blue == null ? new Location(Bukkit.getWorlds().getFirst(), 0.5, 80, 0.5) : blue;
        }
        if (blue == null || total <= 1) {
            return red;
        }

        double ratio = (index + 0.5D) / total;
        World world = red.getWorld() == null ? blue.getWorld() : red.getWorld();
        return new Location(
                world,
                red.getX() + ((blue.getX() - red.getX()) * ratio),
                red.getY() + ((blue.getY() - red.getY()) * ratio),
                red.getZ() + ((blue.getZ() - red.getZ()) * ratio),
                red.getYaw(),
                red.getPitch()
        );
    }
}
