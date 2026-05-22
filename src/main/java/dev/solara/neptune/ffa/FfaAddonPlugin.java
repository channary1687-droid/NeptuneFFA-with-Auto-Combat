package dev.solara.neptune.ffa;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.NeptuneAPIProvider;
import dev.solara.neptune.ffa.arena.FfaArenaService;
import dev.solara.neptune.ffa.anticamp.AntiCampService;
import dev.solara.neptune.ffa.battlepass.BattlepassService;
import dev.solara.neptune.ffa.bounty.BountyService;
import dev.solara.neptune.ffa.combat.AntiKillAbuseService;
import dev.solara.neptune.ffa.combat.CombatConfig;
import dev.solara.neptune.ffa.combat.CombatTagService;
import dev.solara.neptune.ffa.combat.CombatTracker;
import dev.solara.neptune.ffa.combat.PvPToggleService;
import dev.solara.neptune.ffa.command.FfaCommand;
import dev.solara.neptune.ffa.event.FfaEventService;
import dev.solara.neptune.ffa.gui.FfaArenaMenu;
import dev.solara.neptune.ffa.gui.FfaKitMenu;
import dev.solara.neptune.ffa.hotzone.HotzoneService;
import dev.solara.neptune.ffa.koth.KothService;
import dev.solara.neptune.ffa.listener.FfaRuleListener;
import dev.solara.neptune.ffa.listener.PersistentFfaListener;
import dev.solara.neptune.ffa.powerup.PowerupService;
import dev.solara.neptune.ffa.progression.ProgressionService;
import dev.solara.neptune.ffa.quest.QuestService;
import dev.solara.neptune.ffa.rotation.MapRotationService;
import dev.solara.neptune.ffa.season.SeasonService;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.stats.FfaStatsService;
import dev.solara.neptune.ffa.streak.KillstreakService;
import dev.solara.neptune.ffa.ui.FfaUiService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class FfaAddonPlugin extends JavaPlugin {
    private FfaArenaService arenaService;
    private FfaStatsService statsService;
    private BountyService bountyService;
    private HotzoneService hotzoneService;
    private FfaEventService eventService;
    private FfaUiService uiService;
    private AntiCampService antiCampService;
    private KothService kothService;
    private PowerupService powerupService;
    private MapRotationService mapRotationService;
    private FfaRuleListener ruleListener;
    private CombatTagService combatTagService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "scoreboard.yml").exists()) {
            saveResource("scoreboard.yml", false);
        }

        NeptuneAPI neptune = NeptuneAPIProvider.getAPI();
        if (neptune == null) {
            getLogger().severe("Neptune API is not registered. Disabling NeptuneFFAAddon.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        arenaService = new FfaArenaService(this);
        arenaService.load();
        statsService = new FfaStatsService(this);
        statsService.load();

        CombatConfig combatConfig = new CombatConfig(this);
        FfaSessionManager sessionManager = new FfaSessionManager(this, neptune);
        CombatTracker combatTracker = new CombatTracker(this);
        combatTagService = new CombatTagService(this, combatConfig);
        PvPToggleService pvpToggleService = new PvPToggleService(this, combatConfig);
        AntiKillAbuseService antiKillAbuseService = new AntiKillAbuseService(this, combatConfig);

        ProgressionService progressionService = new ProgressionService(this);
        BattlepassService battlepassService = new BattlepassService(this);
        QuestService questService = new QuestService(this, statsService, progressionService);
        SeasonService seasonService = new SeasonService(this);
        bountyService = new BountyService(this, statsService);
        KillstreakService killstreakService = new KillstreakService(this, bountyService);
        hotzoneService = new HotzoneService(this, sessionManager);
        eventService = new FfaEventService(this, sessionManager);
        antiCampService = new AntiCampService(this, sessionManager);
        kothService = new KothService(this, sessionManager, statsService, progressionService, questService, battlepassService);
        powerupService = new PowerupService(this, arenaService, sessionManager);
        mapRotationService = new MapRotationService(this, arenaService, sessionManager);
        uiService = new FfaUiService(this, sessionManager, statsService, combatTracker, eventService, bountyService);
        FfaKitMenu kitMenu = new FfaKitMenu(this, neptune, sessionManager);
        FfaArenaMenu arenaMenu = new FfaArenaMenu(this, neptune, arenaService, sessionManager);
        ruleListener = new FfaRuleListener(this, sessionManager, arenaService);

        FfaCommand command = new FfaCommand(this, neptune, arenaService, sessionManager, kitMenu, statsService,
                eventService, bountyService, questService, battlepassService, seasonService, mapRotationService, arenaMenu,
                uiService, pvpToggleService);
        PluginCommand pluginCommand = getCommand("ffa");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(kitMenu, this);
        getServer().getPluginManager().registerEvents(arenaMenu, this);
        getServer().getPluginManager().registerEvents(new PersistentFfaListener(this, sessionManager, statsService,
                combatTracker, combatTagService, progressionService, killstreakService, bountyService, hotzoneService, eventService,
                questService, battlepassService, antiCampService, pvpToggleService, antiKillAbuseService), this);
        getServer().getPluginManager().registerEvents(ruleListener, this);
        getServer().getPluginManager().registerEvents(powerupService, this);

        hotzoneService.start();
        eventService.start();
        antiCampService.start();
        kothService.start();
        powerupService.start();
        mapRotationService.start();
        ruleListener.start();
        uiService.start();
    }

    @Override
    public void onDisable() {
        if (arenaService != null) {
            arenaService.save();
        }
        if (statsService != null) {
            statsService.save();
        }
        if (bountyService != null) {
            bountyService.shutdown();
        }
        if (hotzoneService != null) {
            hotzoneService.stop();
        }
        if (eventService != null) {
            eventService.stop();
        }
        if (uiService != null) {
            uiService.stop();
        }
        if (antiCampService != null) {
            antiCampService.stop();
        }
        if (kothService != null) {
            kothService.stop();
        }
        if (powerupService != null) {
            powerupService.stop();
        }
        if (mapRotationService != null) {
            mapRotationService.stop();
        }
        if (ruleListener != null) {
            ruleListener.stop();
        }
        if (combatTagService != null) {
            combatTagService.shutdown();
        }
    }
}
