package dev.solara.neptune.ffa.koth;

import dev.solara.neptune.ffa.progression.ProgressionService;
import dev.solara.neptune.ffa.battlepass.BattlepassService;
import dev.solara.neptune.ffa.quest.QuestService;
import dev.solara.neptune.ffa.quest.QuestType;
import dev.solara.neptune.ffa.session.ActiveFfaPlayer;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.stats.FfaStatsService;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KothService {
    private final JavaPlugin plugin;
    private final FfaSessionManager sessions;
    private final FfaStatsService statsService;
    private final ProgressionService progressionService;
    private final QuestService questService;
    private final BattlepassService battlepassService;
    private final Map<UUID, Integer> holdSeconds = new HashMap<>();
    private BukkitTask task;

    public KothService(JavaPlugin plugin, FfaSessionManager sessions, FfaStatsService statsService,
                       ProgressionService progressionService, QuestService questService, BattlepassService battlepassService) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.statsService = statsService;
        this.progressionService = progressionService;
        this.questService = questService;
        this.battlepassService = battlepassService;
    }

    public void start() {
        if (plugin.getConfig().getBoolean("koth.enabled", true)) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        int required = plugin.getConfig().getInt("koth.hold-seconds", 60);
        for (UUID uuid : sessions.activePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            ActiveFfaPlayer active = sessions.active(player);
            if (active == null || !inside(player, active)) {
                holdSeconds.remove(uuid);
                continue;
            }
            questService.add(player, QuestType.HOTZONE_SECONDS, 1);
            int held = holdSeconds.merge(uuid, 1, Integer::sum);
            if (held >= required) {
                holdSeconds.remove(uuid);
                FfaPlayerStats stats = statsService.get(player);
                stats.addCoins(plugin.getConfig().getInt("koth.coins", 300));
                stats.addEventWin();
                stats.addSeasonPoints(5);
                progressionService.addXp(player, stats, plugin.getConfig().getInt("koth.xp", 500));
                battlepassService.addXp(player, stats, plugin.getConfig().getInt("battlepass.xp-per-event-win", 100));
                questService.add(player, QuestType.EVENT_WINS, 1);
                broadcast("koth-captured", "<player>", player.getName());
            }
        }
    }

    private boolean inside(Player player, ActiveFfaPlayer active) {
        Location pos1 = active.arena().pos1();
        Location pos2 = active.arena().pos2();
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || !pos1.getWorld().equals(player.getWorld())) {
            return false;
        }
        Location center = new Location(pos1.getWorld(), (pos1.getX() + pos2.getX()) / 2.0D,
                (pos1.getY() + pos2.getY()) / 2.0D, (pos1.getZ() + pos2.getZ()) / 2.0D);
        return center.distance(player.getLocation()) <= plugin.getConfig().getDouble("koth.radius", 6.0D);
    }

    private void broadcast(String key, String... placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Text.send(player, plugin, key, placeholders);
        }
    }
}
