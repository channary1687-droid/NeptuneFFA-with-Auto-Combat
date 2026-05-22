package dev.solara.neptune.ffa.listener;

import dev.solara.neptune.ffa.bounty.BountyService;
import dev.solara.neptune.ffa.anticamp.AntiCampService;
import dev.solara.neptune.ffa.battlepass.BattlepassService;
import dev.solara.neptune.ffa.combat.AntiKillAbuseService;
import dev.solara.neptune.ffa.combat.CombatConfig;
import dev.solara.neptune.ffa.combat.CombatTagService;
import dev.solara.neptune.ffa.combat.CombatTracker;
import dev.solara.neptune.ffa.combat.PvPToggleService;
import dev.solara.neptune.ffa.event.FfaEventService;
import dev.solara.neptune.ffa.event.FfaEventType;
import dev.solara.neptune.ffa.hotzone.HotzoneService;
import dev.solara.neptune.ffa.progression.ProgressionService;
import dev.solara.neptune.ffa.quest.QuestService;
import dev.solara.neptune.ffa.quest.QuestType;
import dev.solara.neptune.ffa.session.ActiveFfaPlayer;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.stats.FfaStatsService;
import dev.solara.neptune.ffa.streak.KillstreakService;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PersistentFfaListener implements Listener {
    private final JavaPlugin plugin;
    private final CombatConfig combatConfig;
    private final FfaSessionManager sessions;
    private final FfaStatsService statsService;
    private final CombatTracker combatTracker;
    private final CombatTagService combatTagService;
    private final ProgressionService progressionService;
    private final KillstreakService killstreakService;
    private final BountyService bountyService;
    private final HotzoneService hotzoneService;
    private final FfaEventService eventService;
    private final QuestService questService;
    private final BattlepassService battlepassService;
    private final AntiCampService antiCampService;
    private final PvPToggleService pvpToggleService;
    private final AntiKillAbuseService antiKillAbuseService;

    public PersistentFfaListener(JavaPlugin plugin, FfaSessionManager sessions, FfaStatsService statsService,
                                 CombatTracker combatTracker, CombatTagService combatTagService,
                                 ProgressionService progressionService,
                                 KillstreakService killstreakService, BountyService bountyService,
                                 HotzoneService hotzoneService, FfaEventService eventService,
                                 QuestService questService, BattlepassService battlepassService,
                                 AntiCampService antiCampService,
                                 PvPToggleService pvpToggleService,
                                 AntiKillAbuseService antiKillAbuseService) {
        this.plugin = plugin;
        this.combatConfig = new CombatConfig(plugin);
        this.sessions = sessions;
        this.statsService = statsService;
        this.combatTracker = combatTracker;
        this.combatTagService = combatTagService;
        this.progressionService = progressionService;
        this.killstreakService = killstreakService;
        this.bountyService = bountyService;
        this.hotzoneService = hotzoneService;
        this.eventService = eventService;
        this.questService = questService;
        this.battlepassService = battlepassService;
        this.antiCampService = antiCampService;
        this.pvpToggleService = pvpToggleService;
        this.antiKillAbuseService = antiKillAbuseService;
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (!sessions.isActive(event.getPlayer())) {
            return;
        }
        combatTracker.swing(event.getPlayer());
        statsService.get(event.getPlayer()).addSwing();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = attacker(event.getDamager());
        if (attacker == null || !sessions.isActive(attacker) || !sessions.isActive(victim)) {
            return;
        }

        if (inSpawnSafeZone(attacker) || inSpawnSafeZone(victim)) {
            event.setCancelled(true);
            Text.send(attacker, plugin, "spawn-safe-zone");
            return;
        }

        // PvP Toggle Check
        if (!pvpToggleService.isPvPEnabled(attacker)) {
            event.setCancelled(true);
            Text.send(attacker, plugin, "pvp-disabled-attacker");
            return;
        }
        if (!pvpToggleService.isPvPEnabled(victim)) {
            event.setCancelled(true);
            Text.send(attacker, plugin, "pvp-disabled-victim");
            return;
        }

        combatTagService.tag(attacker);
        combatTagService.tag(victim);

        if (combatConfig.pvpBlood()) {
            victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), 20, 0.2, 0.2, 0.2, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
        }

        applyEventDamage(event, victim);

        double finalDamage = Math.max(0.0D, event.getFinalDamage());
        int combo = combatTracker.hit(attacker, victim, finalDamage);
        FfaPlayerStats stats = statsService.get(attacker);
        stats.addHit();
        stats.addDamage(finalDamage);

        int damageXp = (int) Math.round((finalDamage / 2.0D)
                * plugin.getConfig().getInt("progression.xp.damage-per-heart", 2)
                * hotzoneService.xpMultiplier(attacker)
                * antiCampService.rewardMultiplier(attacker));
        progressionService.addXp(attacker, stats, damageXp);
        questService.add(attacker, QuestType.DAMAGE, (int) Math.round(finalDamage));

        if (combo > 1 && combo % 5 == 0) {
            progressionService.addXp(attacker, stats, plugin.getConfig().getInt("progression.xp.combo", 5));
        }
        if (isCritical(attacker)) {
            stats.addCriticalHit();
            progressionService.addXp(attacker, stats, plugin.getConfig().getInt("combat.critical-extra-xp", 5));
            attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 12, 0.3D, 0.4D, 0.3D);
        }
        if (event.getDamager() instanceof Arrow arrow && isHeadshot(arrow, victim)) {
            stats.addHeadshot();
            progressionService.addXp(attacker, stats, plugin.getConfig().getInt("combat.headshot-extra-xp", 10));
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.6F);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!sessions.isActive(victim)) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        Player killer = victim.getKiller();
        if (killer != null && sessions.isActive(killer)) {
            handleKill(killer, victim);
            if (combatConfig.tagUntagOnKill()) {
                combatTagService.untag(killer.getUniqueId());
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> sessions.refreshAfterKill(killer));
        }
        FfaPlayerStats victimStats = statsService.get(victim);
        victimStats.addDeath();
        combatTracker.resetCombo(victim);
        combatTagService.untag(victim.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!sessions.isActive(event.getPlayer())) {
            return;
        }

        Location spawn = sessions.respawnLocation(event.getPlayer());
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> sessions.leaveAfterDeath(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        ActiveFfaPlayer ffa = sessions.active(player);
        if (ffa == null) {
            return;
        }

        if (combatTagService.isTagged(player) && ffa.arena().inSpawnSafeZone(event.getTo())) {
            event.setCancelled(true);
            Text.send(player, plugin, "combat-tag-safe-zone");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatTagService.isTagged(player) || !combatConfig.commandsEnabled()) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        String command = message.split(" ")[0].replace("/", "");
        List<String> list = combatConfig.commandList();
        boolean onList = list.contains(command);

        if (combatConfig.commandsWhitelist()) {
            if (!onList) {
                event.setCancelled(true);
                Text.send(player, plugin, "command-blocked-in-combat");
            }
        } else {
            if (onList) {
                event.setCancelled(true);
                Text.send(player, plugin, "command-blocked-in-combat");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (combatTagService.isTagged(player) && combatConfig.blockTeleport()) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                Text.send(player, plugin, "teleport-blocked-in-combat");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPearl(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        if (combatTagService.isTagged(player)) {
            if (combatConfig.blockEnderPearls()) {
                event.setCancelled(true);
                Text.send(player, plugin, "enderpearl-blocked-in-combat");
                return;
            }
            if (combatConfig.tagEnderPearlRenew()) {
                combatTagService.tag(player);
            }
        } else if (combatConfig.tagSelfTag()) {
            combatTagService.tag(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (combatTagService.isTagged(event.getPlayer()) && combatConfig.blockEat()) {
            event.setCancelled(true);
            Text.send(event.getPlayer(), plugin, "eating-blocked-in-combat");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Item Cooldowns
        if (event.hasItem()) {
            Material material = event.getItem().getType();
            int cooldownSeconds = -1;
            if (combatTagService.isTagged(player)) {
                cooldownSeconds = combatConfig.itemCooldown(material.name(), false);
            }
            if (cooldownSeconds == -1) {
                cooldownSeconds = combatConfig.itemCooldown(material.name(), true);
            }

            if (cooldownSeconds > 0) {
                if (player.getCooldown(material) > 0) {
                    event.setCancelled(true);
                    return;
                }
                player.setCooldown(material, cooldownSeconds * 20);
            }
        }

        // Interaction Blocking
        if (combatTagService.isTagged(player) && combatConfig.interactEnabled()) {
            Material material = event.getClickedBlock() != null ? event.getClickedBlock().getType() : null;
            if (material != null) {
                String name = material.name();
                for (String blocked : combatConfig.interactList()) {
                    if (name.contains(blocked)) {
                        event.setCancelled(true);
                        Text.send(player, plugin, "interaction-blocked-in-combat");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (combatTagService.isTagged(event.getPlayer()) && combatConfig.blockBreak()) {
            event.setCancelled(true);
            Text.send(event.getPlayer(), plugin, "breaking-blocked-in-combat");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (combatTagService.isTagged(event.getPlayer()) && combatConfig.blockPlace()) {
            event.setCancelled(true);
            Text.send(event.getPlayer(), plugin, "placing-blocked-in-combat");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (combatTagService.isTagged(player) && combatConfig.blockGliding() && event.isGliding()) {
            event.setCancelled(true);
            Text.send(player, plugin, "elytra-blocked-in-combat");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatTagService.isTagged(player)) {
            handleCombatLog(player);
        }
        combatTagService.untag(player.getUniqueId());
        sessions.removeSilently(player.getUniqueId());
    }

    private void handleCombatLog(Player player) {
        FfaPlayerStats stats = statsService.get(player);
        
        // Money penalty
        double penalty = combatConfig.logMoneyPenalty();
        if (penalty > 0) {
            int amount = penalty <= 1.0 ? (int) (stats.coins() * penalty) : (int) penalty;
            stats.removeCoins(amount);
        }

        // Kill on logout
        if (combatConfig.logKillOnLogout()) {
            player.setHealth(0);
            stats.addDeath();
        }

        // Commands
        for (String cmd : combatConfig.logCommands()) {
            String processed = cmd.replace("{player}", player.getName());
            // handle {prefix} if needed
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }
    }

    private void handleKill(Player killer, Player victim) {
        if (antiKillAbuseService.check(killer, victim)) {
            return;
        }
        
        FfaPlayerStats stats = statsService.get(killer);
        stats.addKill();

        int killXp = (int) Math.round(plugin.getConfig().getInt("progression.xp.kill", 25)
                * hotzoneService.xpMultiplier(killer)
                * hotzoneService.killMultiplier(killer)
                * antiCampService.rewardMultiplier(killer));
        int coins = (int) Math.round(plugin.getConfig().getInt("progression.coins.kill", 10)
                * hotzoneService.coinsMultiplier(killer)
                * antiCampService.rewardMultiplier(killer));

        stats.addCoins(coins);
        stats.addSeasonPoints(1);
        progressionService.addXp(killer, stats, killXp);
        battlepassService.addXp(killer, stats, plugin.getConfig().getInt("battlepass.xp-per-kill", 15));
        questService.add(killer, QuestType.KILLS, 1);
        questService.add(killer, QuestType.STREAK, stats.currentStreak());
        bountyService.claim(killer, victim);
        killstreakService.handle(killer, stats);
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private boolean inSpawnSafeZone(Player player) {
        ActiveFfaPlayer active = sessions.active(player);
        return active != null && active.arena().inSpawnSafeZone(player.getLocation());
    }

    private void applyEventDamage(EntityDamageByEntityEvent event, Player victim) {
        if (eventService.active(FfaEventType.DOUBLE_DAMAGE)) {
            event.setDamage(event.getDamage() * 2.0D);
        } else if (eventService.active(FfaEventType.ONE_SHOT)) {
            event.setDamage(1000.0D);
        }
        if (eventService.active(FfaEventType.NO_KNOCKBACK)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> victim.setVelocity(victim.getVelocity().zero()));
        }
        if (eventService.active(FfaEventType.EXPLOSIVE_ARROWS) && event.getDamager() instanceof Arrow arrow) {
            arrow.getWorld().createExplosion(arrow.getLocation(), 1.2F, false, false);
        }
        if (eventService.active(FfaEventType.INFINITE_HEALING) && event.getDamager() instanceof Player player) {
            player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), player.getHealth() + 1.0D));
        }
    }

    private boolean isCritical(Player attacker) {
        return attacker.getFallDistance() > 0.0F && !attacker.isOnGround();
    }

    private boolean isHeadshot(Arrow arrow, Player victim) {
        return arrow.getLocation().getY() >= victim.getEyeLocation().getY() - 0.25D;
    }
}
