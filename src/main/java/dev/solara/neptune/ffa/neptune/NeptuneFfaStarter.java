package dev.solara.neptune.ffa.neptune;

import dev.lrxh.api.arena.IArena;
import dev.lrxh.api.kit.IKit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class NeptuneFfaStarter {
    private final JavaPlugin plugin;
    private final Method arenaServiceGet;
    private final Method arenaCopyFrom;
    private final Method arenaCreateDuplicate;
    private final Method kitServiceGet;
    private final Method kitCopyFrom;
    private final Method matchServiceGet;
    private final Method startFfaMatch;
    private final Constructor<?> participantConstructor;

    public NeptuneFfaStarter(JavaPlugin plugin) throws ReflectiveOperationException {
        this.plugin = plugin;

        Class<?> arenaServiceClass = Class.forName("dev.lrxh.neptune.game.arena.ArenaService");
        Class<?> arenaClass = Class.forName("dev.lrxh.neptune.game.arena.Arena");
        Class<?> virtualArenaClass = Class.forName("dev.lrxh.neptune.game.arena.VirtualArena");
        Class<?> kitServiceClass = Class.forName("dev.lrxh.neptune.game.kit.KitService");
        Class<?> kitClass = Class.forName("dev.lrxh.neptune.game.kit.Kit");
        Class<?> matchServiceClass = Class.forName("dev.lrxh.neptune.game.match.MatchService");
        Class<?> participantClass = Class.forName("dev.lrxh.neptune.game.match.impl.participant.Participant");

        this.arenaServiceGet = arenaServiceClass.getMethod("get");
        this.arenaCopyFrom = arenaServiceClass.getMethod("copyFrom", IArena.class);
        this.arenaCreateDuplicate = arenaClass.getMethod("createDuplicate");
        this.kitServiceGet = kitServiceClass.getMethod("get");
        this.kitCopyFrom = kitServiceClass.getMethod("copyFrom", IKit.class);
        this.matchServiceGet = matchServiceClass.getMethod("get");
        this.startFfaMatch = matchServiceClass.getMethod("startMatch", List.class, kitClass, virtualArenaClass);
        this.participantConstructor = participantClass.getConstructor(Player.class);
    }

    @SuppressWarnings("unchecked")
    public void start(List<Player> players, IKit kit, IArena arena, Consumer<Throwable> failureHandler) {
        try {
            Object arenaService = arenaServiceGet.invoke(null);
            Object arenaCopy = arenaCopyFrom.invoke(arenaService, arena);
            CompletableFuture<Object> virtualArenaFuture = (CompletableFuture<Object>) arenaCreateDuplicate.invoke(arenaCopy);

            virtualArenaFuture.whenComplete((virtualArena, throwable) -> {
                if (throwable != null) {
                    runFailure(failureHandler, throwable);
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> startOnMainThread(players, kit, virtualArena, failureHandler));
            });
        } catch (ReflectiveOperationException exception) {
            runFailure(failureHandler, exception);
        }
    }

    private void startOnMainThread(List<Player> players, IKit kit, Object virtualArena, Consumer<Throwable> failureHandler) {
        try {
            Object kitService = kitServiceGet.invoke(null);
            Object kitCopy = kitCopyFrom.invoke(kitService, kit);
            Object matchService = matchServiceGet.invoke(null);
            List<Object> participants = new ArrayList<>();

            for (Player player : players) {
                if (player.isOnline()) {
                    participants.add(participantConstructor.newInstance(player));
                }
            }

            if (participants.size() < 2) {
                throw new IllegalStateException("Not enough online participants to start FFA.");
            }

            startFfaMatch.invoke(matchService, participants, kitCopy, virtualArena);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            runFailure(failureHandler, exception);
        }
    }

    private void runFailure(Consumer<Throwable> failureHandler, Throwable throwable) {
        Bukkit.getScheduler().runTask(plugin, () -> failureHandler.accept(throwable));
    }
}
