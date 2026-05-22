package dev.solara.neptune.ffa.command;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.kit.IKit;
import dev.solara.neptune.ffa.arena.FfaArena;
import dev.solara.neptune.ffa.arena.FfaArenaService;
import dev.solara.neptune.ffa.battlepass.BattlepassService;
import dev.solara.neptune.ffa.bounty.BountyService;
import dev.solara.neptune.ffa.combat.PvPToggleService;
import dev.solara.neptune.ffa.event.FfaEventService;
import dev.solara.neptune.ffa.event.FfaEventType;
import dev.solara.neptune.ffa.gui.FfaArenaMenu;
import dev.solara.neptune.ffa.gui.FfaKitMenu;
import dev.solara.neptune.ffa.neptune.NeptuneCompat;
import dev.solara.neptune.ffa.quest.QuestService;
import dev.solara.neptune.ffa.rotation.MapRotationService;
import dev.solara.neptune.ffa.season.SeasonService;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.stats.FfaPlayerStats;
import dev.solara.neptune.ffa.stats.FfaStatsService;
import dev.solara.neptune.ffa.ui.FfaUiService;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

public final class FfaCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final NeptuneAPI neptune;
    private final FfaArenaService arenas;
    private final FfaSessionManager sessions;
    private final FfaKitMenu kitMenu;
    private final FfaStatsService statsService;
    private final FfaEventService eventService;
    private final BountyService bountyService;
    private final QuestService questService;
    private final BattlepassService battlepassService;
    private final SeasonService seasonService;
    private final MapRotationService mapRotationService;
    private final FfaArenaMenu arenaMenu;
    private final FfaUiService uiService;
    private final PvPToggleService pvpToggleService;

    public FfaCommand(JavaPlugin plugin, NeptuneAPI neptune, FfaArenaService arenas,
                      FfaSessionManager sessions, FfaKitMenu kitMenu, FfaStatsService statsService,
                      FfaEventService eventService, BountyService bountyService, QuestService questService,
                      BattlepassService battlepassService, SeasonService seasonService,
                      MapRotationService mapRotationService, FfaArenaMenu arenaMenu, FfaUiService uiService,
                      PvPToggleService pvpToggleService) {
        this.plugin = plugin;
        this.neptune = neptune;
        this.arenas = arenas;
        this.sessions = sessions;
        this.kitMenu = kitMenu;
        this.statsService = statsService;
        this.eventService = eventService;
        this.bountyService = bountyService;
        this.questService = questService;
        this.battlepassService = battlepassService;
        this.seasonService = seasonService;
        this.mapRotationService = mapRotationService;
        this.arenaMenu = arenaMenu;
        this.uiService = uiService;
        this.pvpToggleService = pvpToggleService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("pvp")) {
            return pvp(sender);
        }

        if (args.length == 0) {
            openDefaultMenu(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> join(sender, args);
            case "leave" -> leave(sender);
            case "list" -> list(sender);
            case "stats" -> stats(sender);
            case "top" -> top(sender, args);
            case "quests" -> quests(sender);
            case "battlepass", "bp" -> battlepass(sender);
            case "season" -> season(sender);
            case "reload" -> reload(sender);
            case "event" -> event(sender, args);
            case "bounty" -> bounty(sender, args);
            case "rotate" -> rotate(sender);
            case "arena" -> arena(sender, args);
            case "editor" -> editor(sender, args);
            default -> openArenaOrUsage(sender, args[0]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>(List.of("join", "leave", "list", "stats", "top", "quests",
                    "battlepass", "bp", "season", "reload", "event", "bounty", "rotate", "arena", "editor"));
            values.addAll(arenaNames());
            return filter(values, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return filter(List.of("kills", "deaths", "streak", "coins", "level", "season"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("event")) {
            List<String> values = new ArrayList<>(List.of("stop"));
            for (FfaEventType type : FfaEventType.values()) {
                values.add(type.name());
            }
            return filter(values, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return filter(arenaNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("join")) {
            return filter(kitNames(), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("arena")) {
            return filter(List.of("create", "delete", "pos1", "pos2", "setspawn", "safezone", "slot", "icon", "flag", "link", "links", "unlink", "list"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("arena")
                && List.of("delete", "pos1", "pos2", "setspawn", "safezone", "slot", "icon", "flag", "link", "links", "unlink").contains(args[1].toLowerCase())) {
            return filter(arenaNames(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("icon")) {
            return filter(materialNames(), args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("flag")) {
            return filter(List.of("allow-place", "allow-break", "auto-regen"), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("flag")) {
            return filter(List.of("allow", "deny"), args[4]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("arena")
                && List.of("link", "links", "unlink").contains(args[1].toLowerCase())) {
            return filter(kitNames(), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("editor")) {
            return filter(arenaNames(), args[1]);
        }
        return Collections.emptyList();
    }

    private void openDefaultMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, plugin, "usage");
            return;
        }

        arenaMenu.openSelector(player);
    }

    private void openArenaOrUsage(CommandSender sender, String arenaName) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, plugin, "usage");
            return;
        }
        if (sessions.isActive(player)) {
            Text.send(player, plugin, "already-in-ffa");
            return;
        }

        FfaArena arena = arenas.get(arenaName);
        if (arena == null) {
            Text.send(sender, plugin, "usage");
            return;
        }
        arenaMenu.joinArena(player, arena);
    }

    private void join(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players can join FFA.");
            return;
        }
        if (sessions.isActive(player)) {
            Text.send(player, plugin, "already-in-ffa");
            return;
        }
        if (args.length < 3) {
            Text.raw(player, plugin, "&cUsage: /ffa join <arena> <kit>");
            return;
        }

        FfaArena arena = arenas.get(args[1]);
        if (arena == null) {
            Text.send(player, plugin, "arena-missing", "<arena>", args[1]);
            return;
        }
        IKit kit = neptune.getKitService().getKitByName(args[2]);
        if (kit == null) {
            Text.send(player, plugin, "unknown-kit", "<kit>", args[2]);
            return;
        }
        if (!arena.linkedKits().contains(kit.getName())) {
            Text.raw(player, plugin, "&cThat kit is not linked to this FFA arena.");
            return;
        }

        sessions.join(player, arena, kit);
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players can leave FFA.");
            return;
        }
        sessions.leave(player);
    }

    private void list(CommandSender sender) {
        if (arenas.all().isEmpty()) {
            Text.raw(sender, plugin, "&7No FFA arenas are configured.");
            return;
        }

        Text.raw(sender, plugin, "&7FFA arenas:");
        for (FfaArena arena : arenas.all()) {
            Text.raw(sender, plugin, "&f" + arena.name()
                    + " &8- &7setup: &f" + arena.isSetup()
                    + " &8- &7slot: &f" + (arena.menuSlot() < 0 ? "auto" : arena.menuSlot())
                    + " &8- &7place: &f" + arena.allowPlace()
                    + " &8- &7break: &f" + arena.allowBreak()
                    + " &8- &7regen: &f" + arena.autoRegen()
                    + " &8- &7safezone: &f" + arena.spawnSafeRadius()
                    + " &8- &7kits: &f" + String.join(", ", arena.linkedKits()));
        }
    }

    private void stats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players have FFA stats.");
            return;
        }

        FfaPlayerStats stats = statsService.get(player);
        Text.send(player, plugin, "stats",
                "<kills>", String.valueOf(stats.kills()),
                "<deaths>", String.valueOf(stats.deaths()),
                "<coins>", String.valueOf(stats.coins()),
                "<level>", String.valueOf(stats.level()),
                "<prestige>", String.valueOf(stats.prestige()));
    }

    private void top(CommandSender sender, String[] args) {
        String type = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "kills";
        ToIntFunction<FfaPlayerStats> metric = switch (type) {
            case "deaths" -> FfaPlayerStats::deaths;
            case "streak" -> FfaPlayerStats::bestStreak;
            case "coins" -> FfaPlayerStats::coins;
            case "level" -> FfaPlayerStats::level;
            case "season" -> FfaPlayerStats::seasonPoints;
            default -> FfaPlayerStats::kills;
        };

        Text.send(sender, plugin, "top-header", "<type>", type);
        List<FfaPlayerStats> sorted = statsService.all().stream()
                .sorted(Comparator.comparingInt(metric).reversed())
                .limit(10)
                .toList();
        int rank = 1;
        for (FfaPlayerStats stats : sorted) {
            Text.send(sender, plugin, "top-line",
                    "<rank>", String.valueOf(rank++),
                    "<player>", stats.name(),
                    "<value>", String.valueOf(metric.applyAsInt(stats)));
        }
    }

    private void quests(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players have quests.");
            return;
        }
        for (String line : questService.summary(player).split("\n")) {
            Text.raw(player, plugin, "&7" + line);
        }
    }

    private void battlepass(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players have battlepass progress.");
            return;
        }
        FfaPlayerStats stats = statsService.get(player);
        Text.send(player, plugin, "battlepass",
                "<tier>", String.valueOf(stats.battlepassTier()),
                "<max>", String.valueOf(plugin.getConfig().getInt("battlepass.max-tier", 50)),
                "<xp>", String.valueOf(stats.battlepassXp()),
                "<needed>", String.valueOf(plugin.getConfig().getInt("battlepass.tier-xp", 250)));
    }

    private void season(CommandSender sender) {
        Text.send(sender, plugin, "season", "<season>", seasonService.current());
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("neptuneffa.admin")) {
            Text.raw(sender, plugin, "&cYou do not have permission.");
            return;
        }
        plugin.reloadConfig();
        arenas.load();
        uiService.reload();
        Text.send(sender, plugin, "reload");
    }

    private void event(CommandSender sender, String[] args) {
        if (!sender.hasPermission("neptuneffa.admin")) {
            Text.raw(sender, plugin, "&cYou do not have permission.");
            return;
        }
        if (args.length < 2) {
            Text.raw(sender, plugin, "&cUsage: /ffa event <type|stop>");
            return;
        }
        if (args[1].equalsIgnoreCase("stop")) {
            eventService.endActive();
            Text.send(sender, plugin, "event-stopped");
            return;
        }
        try {
            FfaEventType type = FfaEventType.valueOf(args[1].toUpperCase(Locale.ROOT));
            eventService.force(type);
            Text.send(sender, plugin, "event-forced", "<event>", type.name());
        } catch (IllegalArgumentException exception) {
            Text.raw(sender, plugin, "&cUnknown event type.");
        }
    }

    private void bounty(CommandSender sender, String[] args) {
        if (!sender.hasPermission("neptuneffa.admin")) {
            Text.raw(sender, plugin, "&cYou do not have permission.");
            return;
        }
        if (args.length < 2) {
            Text.raw(sender, plugin, "&cUsage: /ffa bounty <player>");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            Text.raw(sender, plugin, "&cPlayer not found.");
            return;
        }
        bountyService.mark(target);
        Text.send(sender, plugin, "bounty-forced", "<player>", target.getName());
    }

    private void rotate(CommandSender sender) {
        if (!sender.hasPermission("neptuneffa.admin")) {
            Text.raw(sender, plugin, "&cYou do not have permission.");
            return;
        }
        mapRotationService.rotate();
    }

    private void arena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("neptuneffa.admin")) {
            Text.raw(sender, plugin, "&cYou do not have permission.");
            return;
        }
        if (args.length < 2) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena <create|delete|pos1|pos2|setspawn|safezone|slot|icon|flag|link|unlink|list>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> arenaCreate(sender, args);
            case "delete" -> arenaDelete(sender, args);
            case "pos1" -> arenaPos(sender, args, true);
            case "pos2" -> arenaPos(sender, args, false);
            case "setspawn" -> arenaSetSpawn(sender, args);
            case "safezone" -> arenaSafeZone(sender, args);
            case "slot" -> arenaSlot(sender, args);
            case "icon" -> arenaIcon(sender, args);
            case "flag" -> arenaFlag(sender, args);
            case "link", "links" -> arenaLink(sender, args, true);
            case "unlink" -> arenaLink(sender, args, false);
            case "list" -> list(sender);
            default -> Text.raw(sender, plugin, "&cUsage: /ffa arena <create|delete|pos1|pos2|setspawn|safezone|slot|icon|flag|link|unlink|list>");
        }
    }

    private void arenaCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena create <name>");
            return;
        }
        if (arenas.get(args[2]) != null) {
            Text.send(sender, plugin, "arena-exists", "<arena>", args[2]);
            return;
        }
        arenas.create(args[2]);
        Text.send(sender, plugin, "arena-created", "<arena>", args[2]);
    }

    private void arenaDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena delete <name>");
            return;
        }
        if (!arenas.delete(args[2])) {
            Text.send(sender, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }
        Text.send(sender, plugin, "arena-deleted", "<arena>", args[2]);
    }

    private void arenaPos(CommandSender sender, String[] args, boolean first) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players can set arena positions.");
            return;
        }
        if (args.length < 3) {
            Text.raw(player, plugin, "&cUsage: /ffa arena " + (first ? "pos1" : "pos2") + " <name>");
            return;
        }

        FfaArena arena = arenas.get(args[2]);
        if (arena == null) {
            Text.send(player, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }

        if (first) {
            arena.pos1(player.getLocation());
            Text.send(player, plugin, "arena-pos1", "<arena>", arena.name());
        } else {
            arena.pos2(player.getLocation());
            Text.send(player, plugin, "arena-pos2", "<arena>", arena.name());
        }
        arenas.save();
    }

    private void arenaSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players can set arena spawns.");
            return;
        }
        if (args.length < 3) {
            Text.raw(player, plugin, "&cUsage: /ffa arena setspawn <name>");
            return;
        }

        FfaArena arena = arenas.get(args[2]);
        if (arena == null) {
            Text.send(player, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }

        arena.spawn(player.getLocation().clone());
        arenas.save();
        Text.send(player, plugin, "arena-spawn", "<arena>", arena.name(),
                "<radius>", String.valueOf(arena.spawnSafeRadius()));
    }

    private void arenaSafeZone(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena safezone <name> <radius>");
            return;
        }

        FfaArena arena = arenas.get(args[2]);
        if (arena == null) {
            Text.send(sender, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            Text.raw(sender, plugin, "&cRadius must be a number.");
            return;
        }
        if (radius < 0 || radius > 100) {
            Text.raw(sender, plugin, "&cRadius must be between 0 and 100.");
            return;
        }

        arena.spawnSafeRadius(radius);
        arenas.save();
        Text.send(sender, plugin, "arena-safezone", "<arena>", arena.name(),
                "<radius>", String.valueOf(arena.spawnSafeRadius()));
    }

    private void arenaSlot(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena slot <name> <slot|auto>");
            return;
        }

        FfaArena arena = arenas.get(args[2]);
        if (arena == null) {
            Text.send(sender, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }

        if (args[3].equalsIgnoreCase("auto")) {
            arena.menuSlot(-1);
        } else {
            int slot;
            try {
                slot = Integer.parseInt(args[3]);
            } catch (NumberFormatException exception) {
                Text.raw(sender, plugin, "&cSlot must be a number or auto.");
                return;
            }
            int size = Math.max(9, Math.min(54, plugin.getConfig().getInt("gui.arena-selector.size", 27)));
            size = (size / 9) * 9;
            if (slot < 0 || slot >= size) {
                Text.raw(sender, plugin, "&cSlot must be between 0 and " + (size - 1) + ".");
                return;
            }
            arena.menuSlot(slot);
        }
        arenas.save();
        Text.send(sender, plugin, "arena-slot", "<arena>", arena.name(),
                "<slot>", arena.menuSlot() < 0 ? "auto" : String.valueOf(arena.menuSlot()));
    }

    private void arenaIcon(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena icon <name> [material]");
            return;
        }

        FfaArena arena = arenas.get(args[2]);
        if (arena == null) {
            Text.send(sender, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }

        Material material;
        if (args.length >= 4) {
            material = Material.matchMaterial(args[3]);
        } else if (sender instanceof Player player) {
            material = player.getInventory().getItemInMainHand().getType();
        } else {
            Text.raw(sender, plugin, "&cUsage: /ffa arena icon <name> <material>");
            return;
        }

        if (material == null || material.isAir()) {
            Text.raw(sender, plugin, "&cUnknown or invalid icon material.");
            return;
        }

        arena.icon(material);
        arenas.save();
        Text.send(sender, plugin, "arena-icon", "<arena>", arena.name(), "<icon>", arena.icon().name());
    }

    private void arenaFlag(CommandSender sender, String[] args) {
        if (args.length < 5) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena flag <name> <allow-place|allow-break|auto-regen> <allow|deny>");
            return;
        }

        FfaArena arena = arenas.get(args[2]);
        if (arena == null) {
            Text.send(sender, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }

        boolean enabled;
        if (args[4].equalsIgnoreCase("allow") || args[4].equalsIgnoreCase("true")) {
            enabled = true;
        } else if (args[4].equalsIgnoreCase("deny") || args[4].equalsIgnoreCase("false")) {
            enabled = false;
        } else {
            Text.raw(sender, plugin, "&cUse allow or deny.");
            return;
        }

        switch (args[3].toLowerCase(Locale.ROOT)) {
            case "allow-place", "place" -> arena.allowPlace(enabled);
            case "allow-break", "break" -> arena.allowBreak(enabled);
            case "auto-regen", "regen" -> arena.autoRegen(enabled);
            default -> {
                Text.raw(sender, plugin, "&cUnknown flag. Use allow-place, allow-break, or auto-regen.");
                return;
            }
        }

        arenas.save();
        Text.send(sender, plugin, "arena-flag", "<arena>", arena.name(),
                "<flag>", args[3], "<state>", enabled ? "ALLOW" : "DENY");
    }

    private void editor(CommandSender sender, String[] args) {
        if (!sender.hasPermission("neptuneffa.admin")) {
            Text.raw(sender, plugin, "&cYou do not have permission.");
            return;
        }
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players can open the FFA editor.");
            return;
        }
        if (args.length >= 2) {
            FfaArena arena = arenas.get(args[1]);
            if (arena == null) {
                Text.send(player, plugin, "arena-missing", "<arena>", args[1]);
                return;
            }
            arenaMenu.openArenaEditor(player, arena);
            return;
        }
        arenaMenu.openEditor(player);
    }

    private void arenaLink(CommandSender sender, String[] args, boolean link) {
        if (args.length < 4) {
            Text.raw(sender, plugin, "&cUsage: /ffa arena " + (link ? "link" : "unlink") + " <name> <kit>");
            return;
        }

        FfaArena arena = arenas.get(args[2]);
        if (arena == null) {
            Text.send(sender, plugin, "arena-missing", "<arena>", args[2]);
            return;
        }
        IKit kit = neptune.getKitService().getKitByName(args[3]);
        if (kit == null) {
            Text.send(sender, plugin, "unknown-kit", "<kit>", args[3]);
            return;
        }

        if (link) {
            arena.linkedKits().add(kit.getName());
            Text.send(sender, plugin, "arena-linked", "<arena>", arena.name(), "<kit>", kit.getName());
        } else {
            arena.linkedKits().remove(kit.getName());
            Text.send(sender, plugin, "arena-unlinked", "<arena>", arena.name(), "<kit>", kit.getName());
        }
        arenas.save();
    }

    private boolean pvp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.raw(sender, plugin, "&cOnly players can toggle PvP.");
            return true;
        }
        pvpToggleService.toggle(player);
        boolean enabled = pvpToggleService.isPvPEnabled(player);
        Text.raw(player, plugin, "&7PvP is now " + (enabled ? "&aenabled" : "&cdisabled") + "&7.");
        return true;
    }

    private List<String> arenaNames() {
        List<String> names = new ArrayList<>();
        for (FfaArena arena : arenas.all()) {
            names.add(arena.name());
        }
        return names;
    }

    private List<String> kitNames() {
        List<String> names = new ArrayList<>();
        for (IKit kit : NeptuneCompat.allKits(neptune)) {
            names.add(kit.getName());
        }
        return names;
    }

    private List<String> materialNames() {
        List<String> names = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) {
                names.add(material.name());
            }
        }
        return names;
    }

    private List<String> filter(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return values.stream()
                .filter(value -> value.toLowerCase().startsWith(lowerPrefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
