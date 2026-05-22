package dev.solara.neptune.ffa;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.kit.IKit;
import dev.solara.neptune.ffa.neptune.NeptuneCompat;
import dev.solara.neptune.ffa.neptune.NeptuneFfaStarter;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FfaLobbyManager {
    private final JavaPlugin plugin;
    private final NeptuneAPI neptune;
    private final NeptuneFfaStarter starter;
    private final Map<String, FfaLobby> lobbies = new HashMap<>();
    private final Map<UUID, FfaLobbyKey> queuedPlayers = new HashMap<>();

    public FfaLobbyManager(JavaPlugin plugin, NeptuneAPI neptune, NeptuneFfaStarter starter) {
        this.plugin = plugin;
        this.neptune = neptune;
        this.starter = starter;
    }

    public void join(Player player, String kitName, String arenaName) {
        if (queuedPlayers.containsKey(player.getUniqueId())) {
            Text.send(player, plugin, "already-queued");
            return;
        }

        IKit kit = neptune.getKitService().getKitByName(kitName);
        if (kit == null) {
            Text.send(player, plugin, "unknown-kit", "<kit>", kitName);
            return;
        }

        IArena arena = arenaName == null ? findArena(kit) : neptune.getArenaService().getArenaByName(arenaName);
        if (arena == null) {
            Text.send(player, plugin, arenaName == null ? "no-arena" : "unknown-arena", "<arena>", String.valueOf(arenaName));
            return;
        }
        if (!arena.isEnabled() || !arena.isSetup()) {
            Text.send(player, plugin, "no-arena");
            return;
        }

        FfaLobbyKey key = new FfaLobbyKey(kit.getName(), arena.getName());
        FfaLobby lobby = lobbies.computeIfAbsent(key.id(), ignored -> new FfaLobby(key, kit, arena));
        int maxPlayers = maxPlayers();
        if (lobby.players().size() >= maxPlayers) {
            Text.raw(player, plugin, "&cThat FFA queue is full.");
            return;
        }

        lobby.players().add(player.getUniqueId());
        queuedPlayers.put(player.getUniqueId(), key);

        Text.send(player, plugin, "joined",
                "<kit>", kit.getName(),
                "<arena>", arena.getName(),
                "<count>", String.valueOf(lobby.players().size()),
                "<max>", String.valueOf(maxPlayers));

        if (lobby.players().size() >= maxPlayers) {
            start(lobby, false);
            return;
        }

        if (lobby.players().size() >= minPlayers() && lobby.countdownTask() == null) {
            startCountdown(lobby);
        }
    }

    public void leave(Player player) {
        FfaLobbyKey key = queuedPlayers.remove(player.getUniqueId());
        if (key == null) {
            Text.send(player, plugin, "not-queued");
            return;
        }

        FfaLobby lobby = lobbies.get(key.id());
        if (lobby != null) {
            lobby.players().remove(player.getUniqueId());
            if (lobby.players().isEmpty()) {
                cancelLobby(lobby);
                lobbies.remove(key.id());
            }
        }

        Text.send(player, plugin, "left");
    }

    public void forceStart(Player sender, String kitName, String arenaName) {
        IKit kit = neptune.getKitService().getKitByName(kitName);
        if (kit == null) {
            Text.send(sender, plugin, "unknown-kit", "<kit>", kitName);
            return;
        }

        IArena arena = arenaName == null ? findArena(kit) : neptune.getArenaService().getArenaByName(arenaName);
        if (arena == null) {
            Text.send(sender, plugin, arenaName == null ? "no-arena" : "unknown-arena", "<arena>", String.valueOf(arenaName));
            return;
        }

        FfaLobbyKey key = new FfaLobbyKey(kit.getName(), arena.getName());
        FfaLobby lobby = lobbies.get(key.id());
        if (lobby == null || lobby.players().size() < 2) {
            Text.raw(sender, plugin, "&cThat FFA queue needs at least 2 players.");
            return;
        }

        start(lobby, true);
    }

    public Collection<FfaLobby> lobbies() {
        return lobbies.values();
    }

    public void cancelAll() {
        for (FfaLobby lobby : lobbies.values()) {
            cancelLobby(lobby);
        }
        lobbies.clear();
        queuedPlayers.clear();
    }

    private IArena findArena(IKit kit) {
        for (IArena arena : kit.getAllArenas()) {
            if (arena != null && arena.isEnabled() && arena.isSetup()) {
                return arena;
            }
        }
        for (IArena arena : NeptuneCompat.allArenas(neptune)) {
            if (arena != null && arena.isEnabled() && arena.isSetup()) {
                return arena;
            }
        }
        return null;
    }

    private void startCountdown(FfaLobby lobby) {
        final int[] seconds = {plugin.getConfig().getInt("auto-start-seconds", 10)};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (lobby.players().size() < minPlayers()) {
                broadcast(lobby, "countdown-cancelled");
                cancelLobby(lobby);
                return;
            }

            if (seconds[0] <= 0) {
                start(lobby, false);
                return;
            }

            if (seconds[0] <= 5 || seconds[0] % 5 == 0) {
                broadcast(lobby, "countdown",
                        "<kit>", lobby.kit().getName(),
                        "<seconds>", String.valueOf(seconds[0]),
                        "<count>", String.valueOf(lobby.players().size()));
            }
            seconds[0]--;
        }, 0L, 20L);
        lobby.countdownTask(task);
    }

    private void start(FfaLobby lobby, boolean forced) {
        if (!forced && lobby.players().size() < minPlayers()) {
            return;
        }

        cancelLobby(lobby);
        lobbies.remove(lobby.key().id());

        List<Player> players = new ArrayList<>();
        for (UUID uuid : lobby.players()) {
            queuedPlayers.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }

        if (players.size() < 2) {
            for (Player player : players) {
                Text.raw(player, plugin, "&cNot enough online players to start FFA.");
            }
            return;
        }

        for (Player player : players) {
            Text.send(player, plugin, "starting", "<count>", String.valueOf(players.size()));
        }

        starter.start(players, lobby.kit(), lobby.arena(), throwable -> {
            plugin.getLogger().severe("Failed to start FFA match: " + throwable.getMessage());
            throwable.printStackTrace();
            for (Player player : players) {
                if (player.isOnline()) {
                    Text.send(player, plugin, "start-failed");
                }
            }
        });
    }

    private void broadcast(FfaLobby lobby, String messagePath, String... placeholders) {
        for (UUID uuid : lobby.players()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Text.send(player, plugin, messagePath, placeholders);
            }
        }
    }

    private void cancelLobby(FfaLobby lobby) {
        if (lobby.countdownTask() != null) {
            lobby.countdownTask().cancel();
            lobby.countdownTask(null);
        }
    }

    private int minPlayers() {
        return Math.max(2, plugin.getConfig().getInt("min-players", 2));
    }

    private int maxPlayers() {
        return Math.max(minPlayers(), plugin.getConfig().getInt("max-players", 12));
    }
}
