package dev.solara.neptune.ffa;

import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.kit.IKit;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashSet;
import java.util.UUID;

public final class FfaLobby {
    private final FfaLobbyKey key;
    private final IKit kit;
    private final IArena arena;
    private final LinkedHashSet<UUID> players = new LinkedHashSet<>();
    private BukkitTask countdownTask;

    FfaLobby(FfaLobbyKey key, IKit kit, IArena arena) {
        this.key = key;
        this.kit = kit;
        this.arena = arena;
    }

    public FfaLobbyKey key() {
        return key;
    }

    public IKit kit() {
        return kit;
    }

    public IArena arena() {
        return arena;
    }

    public LinkedHashSet<UUID> players() {
        return players;
    }

    BukkitTask countdownTask() {
        return countdownTask;
    }

    void countdownTask(BukkitTask countdownTask) {
        this.countdownTask = countdownTask;
    }
}
