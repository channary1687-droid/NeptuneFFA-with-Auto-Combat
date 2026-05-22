package dev.solara.neptune.ffa.gui;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.kit.IKit;
import dev.solara.neptune.ffa.arena.FfaArena;
import dev.solara.neptune.ffa.arena.FfaArenaService;
import dev.solara.neptune.ffa.session.FfaSessionManager;
import dev.solara.neptune.ffa.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FfaArenaMenu implements Listener {
    private final JavaPlugin plugin;
    private final NeptuneAPI neptune;
    private final FfaArenaService arenas;
    private final FfaSessionManager sessions;

    public FfaArenaMenu(JavaPlugin plugin, NeptuneAPI neptune, FfaArenaService arenas, FfaSessionManager sessions) {
        this.plugin = plugin;
        this.neptune = neptune;
        this.arenas = arenas;
        this.sessions = sessions;
    }

    public void openSelector(Player player) {
        if (sessions.isActive(player)) {
            Text.send(player, plugin, "already-in-ffa");
            return;
        }

        int size = inventorySize(plugin.getConfig().getInt("gui.arena-selector.size", 27), 27);
        MenuHolder holder = new MenuHolder(MenuType.SELECTOR, null);
        Inventory inventory = Bukkit.createInventory(holder, size,
                Text.color(plugin.getConfig().getString("gui.arena-selector.title", "&bFFA Arenas")));
        fill(inventory);

        int autoSlot = 0;
        for (FfaArena arena : arenas.all()) {
            int slot = arena.menuSlot();
            if (slot < 0 || slot >= size || holder.arenasBySlot.containsKey(slot)) {
                slot = nextOpenSlot(inventory, autoSlot);
            }
            if (slot < 0) {
                break;
            }
            autoSlot = slot + 1;

            holder.arenasBySlot.put(slot, arena.name());
            inventory.setItem(slot, arenaItem(arena, false));
        }

        player.openInventory(inventory);
    }

    public void openEditor(Player player) {
        int size = inventorySize(plugin.getConfig().getInt("editor.size", 54), 54);
        MenuHolder holder = new MenuHolder(MenuType.EDITOR_LIST, null);
        Inventory inventory = Bukkit.createInventory(holder, size, Text.color("&bFFA Arena Editor"));
        fill(inventory);

        int slot = 0;
        for (FfaArena arena : arenas.all()) {
            slot = nextOpenSlot(inventory, slot);
            if (slot < 0) {
                break;
            }
            holder.arenasBySlot.put(slot, arena.name());
            inventory.setItem(slot, arenaItem(arena, true));
            slot++;
        }

        player.openInventory(inventory);
    }

    public void openArenaEditor(Player player, FfaArena arena) {
        MenuHolder holder = new MenuHolder(MenuType.EDITOR_ARENA, arena);
        Inventory inventory = Bukkit.createInventory(holder, 27, Text.color("&bEdit " + arena.name()));
        fill(inventory);

        setButton(inventory, holder, 10, "toggle-place", Material.GRASS_BLOCK, "&bPlace Blocks",
                "&7Current: &f" + state(arena.allowPlace()),
                "&7Click to toggle.");
        setButton(inventory, holder, 11, "toggle-break", Material.IRON_PICKAXE, "&bBreak Blocks",
                "&7Current: &f" + state(arena.allowBreak()),
                "&7Click to toggle.");
        setButton(inventory, holder, 12, "toggle-regen", Material.CLOCK, "&bAuto Regen",
                "&7Current: &f" + state(arena.autoRegen()),
                "&7Restores tracked changes every 5 minutes.",
                "&7Click to toggle.");
        setButton(inventory, holder, 13, "set-spawn", Material.ENDER_PEARL, "&bSet Spawn",
                "&7Uses your current location.",
                "&7Click to set.");
        setButton(inventory, holder, 14, "set-pos1", Material.LIME_WOOL, "&bSet Pos1",
                "&7Uses your current location.",
                "&7Click to set.");
        setButton(inventory, holder, 15, "set-pos2", Material.RED_WOOL, "&bSet Pos2",
                "&7Uses your current location.",
                "&7Click to set.");
        setButton(inventory, holder, 16, "icon", arena.icon(), "&bArena Icon",
                "&7Current: &f" + arena.icon().name(),
                "&7Hold an item and click to set.",
                "&7This icon shows in /ffa.");
        setButton(inventory, holder, 19, "slot", Material.ITEM_FRAME, "&b/ffa Menu Slot",
                "&7Current: &f" + (arena.menuSlot() < 0 ? "auto" : arena.menuSlot()),
                "&7Click to choose from the /ffa grid.",
                "&7Shift click to set auto.");
        setButton(inventory, holder, 22, "delete", Material.BARRIER, "&cDelete Arena",
                "&7Deletes this FFA arena.",
                "&7Click to delete.");
        setButton(inventory, holder, 26, "back", Material.ARROW, "&bBack",
                "&7Return to arena list.");

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        if (holder.type == MenuType.SELECTOR) {
            joinArena(player, holder.arenasBySlot.get(event.getRawSlot()));
            return;
        }
        if (holder.type == MenuType.SLOT_PICKER) {
            if (holder.arena == null) {
                return;
            }
            if (event.isShiftClick()) {
                holder.arena.menuSlot(-1);
                arenas.save();
                Text.send(player, plugin, "arena-slot", "<arena>", holder.arena.name(), "<slot>", "auto");
                openArenaEditor(player, holder.arena);
                return;
            }
            assignMenuSlot(player, holder.arena, event.getRawSlot());
            return;
        }
        if (holder.type == MenuType.EDITOR_LIST) {
            String arenaName = holder.arenasBySlot.get(event.getRawSlot());
            if (arenaName == null) {
                return;
            }
            if (event.isRightClick()) {
                deleteArena(player, arenaName);
                openEditor(player);
                return;
            }
            FfaArena arena = arenas.get(arenaName);
            if (arena != null) {
                openArenaEditor(player, arena);
            }
            return;
        }

        String action = holder.actionsBySlot.get(event.getRawSlot());
        if (action == null || holder.arena == null) {
            return;
        }
        handleEditorAction(player, holder.arena, action, event.isRightClick(), event.isShiftClick());
    }

    public void joinArena(Player player, FfaArena arena) {
        if (arena == null) {
            return;
        }
        joinArena(player, arena.name());
    }

    private void joinArena(Player player, String arenaName) {
        if (arenaName == null) {
            return;
        }
        FfaArena arena = arenas.get(arenaName);
        if (arena == null) {
            return;
        }
        if (sessions.isActive(player)) {
            Text.send(player, plugin, "already-in-ffa");
            return;
        }
        if (!arena.isSetup()) {
            Text.send(player, plugin, "arena-not-setup");
            return;
        }

        IKit kit = firstLinkedKit(arena);
        if (kit == null) {
            Text.send(player, plugin, "no-linked-kits");
            return;
        }

        player.closeInventory();
        sessions.join(player, arena, kit);
    }

    private void handleEditorAction(Player player, FfaArena arena, String action, boolean rightClick, boolean shiftClick) {
        switch (action) {
            case "toggle-place" -> {
                arena.allowPlace(!arena.allowPlace());
                arenas.save();
                Text.raw(player, plugin, "&aPlace blocks: &f" + state(arena.allowPlace()));
                openArenaEditor(player, arena);
            }
            case "toggle-break" -> {
                arena.allowBreak(!arena.allowBreak());
                arenas.save();
                Text.raw(player, plugin, "&aBreak blocks: &f" + state(arena.allowBreak()));
                openArenaEditor(player, arena);
            }
            case "toggle-regen" -> {
                arena.autoRegen(!arena.autoRegen());
                arenas.save();
                Text.raw(player, plugin, "&aAuto regen: &f" + state(arena.autoRegen()));
                openArenaEditor(player, arena);
            }
            case "set-spawn" -> {
                arena.spawn(player.getLocation().clone());
                arenas.save();
                Text.send(player, plugin, "arena-spawn", "<arena>", arena.name());
                openArenaEditor(player, arena);
            }
            case "set-pos1" -> {
                arena.pos1(player.getLocation().clone());
                arenas.save();
                Text.send(player, plugin, "arena-pos1", "<arena>", arena.name());
                openArenaEditor(player, arena);
            }
            case "set-pos2" -> {
                arena.pos2(player.getLocation().clone());
                arenas.save();
                Text.send(player, plugin, "arena-pos2", "<arena>", arena.name());
                openArenaEditor(player, arena);
            }
            case "icon" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    Text.raw(player, plugin, "&cHold an item in your main hand to set the arena icon.");
                    return;
                }
                arena.icon(hand.getType());
                arenas.save();
                Text.send(player, plugin, "arena-icon", "<arena>", arena.name(), "<icon>", arena.icon().name());
                openArenaEditor(player, arena);
            }
            case "slot" -> {
                if (shiftClick) {
                    arena.menuSlot(-1);
                    arenas.save();
                    Text.send(player, plugin, "arena-slot", "<arena>", arena.name(), "<slot>", "auto");
                    openArenaEditor(player, arena);
                    return;
                }
                openSlotPicker(player, arena);
            }
            case "delete" -> {
                String name = arena.name();
                deleteArena(player, name);
                openEditor(player);
            }
            case "back" -> openEditor(player);
            default -> {
            }
        }
    }

    private void deleteArena(Player player, String arenaName) {
        if (arenas.delete(arenaName)) {
            Text.send(player, plugin, "arena-deleted", "<arena>", arenaName);
        }
    }

    private void openSlotPicker(Player player, FfaArena target) {
        int size = inventorySize(plugin.getConfig().getInt("gui.arena-selector.size", 27), 27);
        MenuHolder holder = new MenuHolder(MenuType.SLOT_PICKER, target);
        Inventory inventory = Bukkit.createInventory(holder, size, Text.color("&bSet Slot: " + target.name()));
        fill(inventory);

        int autoSlot = 0;
        for (FfaArena arena : arenas.all()) {
            int slot = arena.menuSlot();
            if (slot < 0 || slot >= size || holder.arenasBySlot.containsKey(slot)) {
                slot = nextOpenSlot(inventory, autoSlot);
            }
            if (slot < 0) {
                break;
            }
            autoSlot = slot + 1;

            holder.arenasBySlot.put(slot, arena.name());
            inventory.setItem(slot, slotPickerItem(arena, target));
        }

        player.openInventory(inventory);
    }

    private void assignMenuSlot(Player player, FfaArena target, int slot) {
        for (FfaArena arena : arenas.all()) {
            if (!arena.name().equalsIgnoreCase(target.name()) && arena.menuSlot() == slot) {
                arena.menuSlot(-1);
            }
        }

        target.menuSlot(slot);
        arenas.save();
        Text.send(player, plugin, "arena-slot", "<arena>", target.name(), "<slot>", String.valueOf(slot));
        openArenaEditor(player, target);
    }

    private ItemStack arenaItem(FfaArena arena, boolean editor) {
        Material material = arena.isSetup() ? arena.icon() : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(replace(plugin.getConfig().getString("gui.arena-selector.item.name", "&b<arena>"), arena)));
            List<String> source = editor
                    ? List.of("&7Setup: &f<setup>", "&7Slot: &f<slot>", "&7Place: &f<place> &8| &7Break: &f<break>", "&7Auto regen: &f<regen>", "", "&aLeft click to edit", "&cRight click to delete")
                    : plugin.getConfig().getStringList("gui.arena-selector.item.lore");
            if (source.isEmpty()) {
                source = List.of("&7Playing: &f<playing>", "&7Kits: &f<kits>", "&aClick to join");
            }
            List<String> lore = new ArrayList<>();
            for (String line : source) {
                lore.add(Text.color(replace(line, arena)));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String replace(String line, FfaArena arena) {
        return line
                .replace("<arena>", arena.name())
                .replace("<setup>", String.valueOf(arena.isSetup()))
                .replace("<playing>", String.valueOf(sessions.playing(arena)))
                .replace("<kits>", String.valueOf(arena.linkedKits().size()))
                .replace("<place>", state(arena.allowPlace()))
                .replace("<break>", state(arena.allowBreak()))
                .replace("<regen>", state(arena.autoRegen()))
                .replace("<slot>", arena.menuSlot() < 0 ? "auto" : String.valueOf(arena.menuSlot()));
    }

    private ItemStack slotPickerItem(FfaArena arena, FfaArena target) {
        ItemStack item = arenaItem(arena, false);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add(Text.color(""));
            if (arena.name().equalsIgnoreCase(target.name())) {
                lore.add(Text.color("&eCurrent selected arena."));
            } else {
                lore.add(Text.color("&7Click to use this slot for &f" + target.name() + "&7."));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private IKit firstLinkedKit(FfaArena arena) {
        for (String kitName : arena.linkedKits()) {
            IKit kit = neptune.getKitService().getKitByName(kitName);
            if (kit != null) {
                return kit;
            }
        }
        return null;
    }

    private void setButton(Inventory inventory, MenuHolder holder, int slot, String action, Material material,
                           String name, String... lore) {
        holder.actionsBySlot.put(slot, action);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Text.color(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void fill(Inventory inventory) {
        if (!plugin.getConfig().getBoolean("gui.fill-empty", true)) {
            return;
        }
        ItemStack filler = new ItemStack(material(plugin.getConfig().getString("gui.filler-material", "GRAY_STAINED_GLASS_PANE")));
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private int nextOpenSlot(Inventory inventory, int start) {
        for (int slot = Math.max(0, start); slot < inventory.getSize(); slot++) {
            if (isFiller(inventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isFiller(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && " ".equals(meta.getDisplayName());
    }

    private int inventorySize(int configured, int fallback) {
        int size = configured <= 0 ? fallback : configured;
        size = Math.max(9, Math.min(54, size));
        return (size / 9) * 9;
    }

    private Material material(String name) {
        Material material = Material.matchMaterial(name);
        return material == null ? Material.GRAY_STAINED_GLASS_PANE : material;
    }

    private String state(boolean enabled) {
        return enabled ? "ALLOW" : "DENY";
    }

    private enum MenuType {
        SELECTOR,
        EDITOR_LIST,
        EDITOR_ARENA,
        SLOT_PICKER
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private final FfaArena arena;
        private final Map<Integer, String> arenasBySlot = new HashMap<>();
        private final Map<Integer, String> actionsBySlot = new HashMap<>();

        private MenuHolder(MenuType type, FfaArena arena) {
            this.type = type;
            this.arena = arena;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
