package dev.solara.neptune.ffa.ui;

import dev.solara.neptune.ffa.bounty.BountyService;
import dev.solara.neptune.ffa.combat.CombatSnapshot;
import dev.solara.neptune.ffa.combat.CombatTracker;
import dev.solara.neptune.ffa.event.FfaEventService;
import dev.solara.neptune.ffa.session.ActiveFfaPlayer;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.stats.FfaStatsService;
import dev.solara.neptune.ffa.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FfaUiService {
    private final JavaPlugin plugin;
    private final FfaSessionManager sessions;
    private final FfaStatsService statsService;
    private final CombatTracker combatTracker;
    private final FfaEventService eventService;
    private final BountyService bountyService;
    private final Map<UUID, BossBar> eventBars = new HashMap<>();
    private FileConfiguration scoreboardConfig;
    private BukkitTask task;

    public FfaUiService(JavaPlugin plugin, FfaSessionManager sessions, FfaStatsService statsService,
                        CombatTracker combatTracker, FfaEventService eventService, BountyService bountyService) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.statsService = statsService;
        this.combatTracker = combatTracker;
        this.eventService = eventService;
        this.bountyService = bountyService;
    }

    public void start() {
        reload();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void reload() {
        scoreboardConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "scoreboard.yml"));
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (BossBar bar : eventBars.values()) {
            bar.removeAll();
        }
        eventBars.clear();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ActiveFfaPlayer active = sessions.active(player);
            if (active == null) {
                removeBossbar(player);
                removeScoreboard(player);
                continue;
            }

            FfaPlayerStats stats = statsService.get(player);
            CombatSnapshot combat = combatTracker.snapshot(player);
            if (scoreboardConfig.getBoolean("enabled", plugin.getConfig().getBoolean("ui.scoreboard", true))) {
                scoreboard(player, active, stats);
            } else {
                removeScoreboard(player);
            }
            if (plugin.getConfig().getBoolean("ui.actionbar", true)) {
                actionbar(player, combat);
            }
            if (plugin.getConfig().getBoolean("ui.bossbar", true)) {
                bossbar(player);
            }
        }
    }

    private void scoreboard(Player player, ActiveFfaPlayer active, FfaPlayerStats stats) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("ffa", "dummy",
                Text.color(scoreboardConfig.getString("title", "&b&lFFA")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = scoreboardConfig.getStringList("lines");
        if (lines.isEmpty()) {
            lines = List.of(
                    "&7Arena: &f<arena>",
                    "&7Kit: &f<kit>",
                    "&7Kills: &f<kills>",
                    "&7Deaths: &f<deaths>",
                    "&7Streak: &f<streak>",
                    "&7Coins: &f<coins>",
                    "&7Level: &f<level>",
                    "&7Prestige: &f<prestige>"
            );
        }

        int score = lines.size();
        for (String line : lines) {
            objective.getScore(uniqueLine(Text.color(replaceScoreboard(line, active, stats)), score)).setScore(score);
            score--;
        }
        player.setScoreboard(board);
    }

    private String replaceScoreboard(String line, ActiveFfaPlayer active, FfaPlayerStats stats) {
        return line
                .replace("<arena>", active.arena().name())
                .replace("<kit>", active.kit().getName())
                .replace("<kills>", String.valueOf(stats.kills()))
                .replace("<deaths>", String.valueOf(stats.deaths()))
                .replace("<streak>", String.valueOf(stats.currentStreak()))
                .replace("<best_streak>", String.valueOf(stats.bestStreak()))
                .replace("<coins>", String.valueOf(stats.coins()))
                .replace("<level>", String.valueOf(stats.level()))
                .replace("<prestige>", String.valueOf(stats.prestige()))
                .replace("<season_points>", String.valueOf(stats.seasonPoints()))
                .replace("<battlepass_tier>", String.valueOf(stats.battlepassTier()))
                .replace("<battlepass_xp>", String.valueOf(stats.battlepassXp()));
    }

    private void actionbar(Player player, CombatSnapshot combat) {
        String format = plugin.getConfig().getString("ui.actionbar-format", "&bCombo: &f<combo>");
        String message = format
                .replace("<combo>", String.valueOf(combat.combo()))
                .replace("<cps>", String.valueOf(combat.cps()))
                .replace("<reach>", String.format("%.2f", combat.reach()))
                .replace("<accuracy>", String.format("%.1f", combat.accuracy()));
        player.sendActionBar(Component.text(Text.color(message)));
    }

    private void bossbar(Player player) {
        BossBar bar = eventBars.computeIfAbsent(player.getUniqueId(), ignored -> {
            BossBar created = Bukkit.createBossBar("FFA", BarColor.BLUE, BarStyle.SOLID);
            created.addPlayer(player);
            return created;
        });

        if (eventService.active() != null) {
            bar.setTitle("EVENT: " + eventService.active().name().replace('_', ' '));
            bar.setColor(BarColor.PURPLE);
            bar.setVisible(true);
        } else if (bountyService.isBounty(player)) {
            bar.setTitle("BOUNTY ACTIVE");
            bar.setColor(BarColor.RED);
            bar.setVisible(true);
        } else {
            bar.setVisible(false);
        }
    }

    private void removeBossbar(Player player) {
        BossBar bar = eventBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
        }
    }

    private void removeScoreboard(Player player) {
        if (player.getScoreboard().getObjective("ffa") == null) {
            return;
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private String uniqueLine(String line, int score) {
        if (line.length() > 36) {
            line = line.substring(0, 36);
        }
        return line + ChatColor.values()[score % ChatColor.values().length];
    }
}
