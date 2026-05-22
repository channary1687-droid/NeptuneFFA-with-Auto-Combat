package dev.solara.neptune.ffa.gui;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.kit.IKit;
import dev.solara.neptune.ffa.arena.FfaArena;
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

public final class FfaKitMenu implements Listener {
    private final JavaPlugin plugin;
    private final NeptuneAPI neptune;
    private final FfaSessionManager sessions;

    public FfaKitMenu(JavaPlugin plugin, NeptuneAPI neptune, FfaSessionManager sessions) {
        this.plugin = plugin;
        this.neptune = neptune;
        this.sessions = sessions;
    }

    public void open(Player player, FfaArena arena) {
        if (sessions.isActive(player)) {
            Text.send(player, plugin, "already-in-ffa");
            return;
        }
        if (arena == null) {
            Text.raw(player, plugin, "&cNo FFA arena is configured.");
            return;
        }
        if (!arena.isSetup()) {
            Text.send(player, plugin, "arena-not-setup");
            return;
        }
        if (arena.linkedKits().isEmpty()) {
            Text.send(player, plugin, "no-linked-kits");
            return;
        }

        int size = Math.max(9, Math.min(54, plugin.getConfig().getInt("gui.size", 54)));
        size = (size / 9) * 9;
        MenuHolder holder = new MenuHolder(arena);
        Inventory inventory = Bukkit.createInventory(holder, size, Text.color(plugin.getConfig().getString("gui.title", "&bFFA Kits")));

        if (plugin.getConfig().getBoolean("gui.fill-empty", true)) {
            ItemStack filler = new ItemStack(material(plugin.getConfig().getString("gui.filler-material", "GRAY_STAINED_GLASS_PANE")));
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                filler.setItemMeta(meta);
            }
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, filler);
            }
        }

        List<Integer> configuredSlots = plugin.getConfig().getIntegerList("gui.kit-slots");
        int autoSlot = 0;
        for (String kitName : arena.linkedKits()) {
            IKit kit = neptune.getKitService().getKitByName(kitName);
            if (kit == null) {
                continue;
            }

            int slot = nextSlot(configuredSlots, autoSlot, size);
            if (slot < 0) {
                break;
            }
            autoSlot++;

            holder.kitsBySlot.put(slot, kit.getName());
            inventory.setItem(slot, itemFor(kit, arena));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String kitName = holder.kitsBySlot.get(event.getRawSlot());
        if (kitName == null) {
            return;
        }

        IKit kit = neptune.getKitService().getKitByName(kitName);
        if (kit == null) {
            Text.send(player, plugin, "unknown-kit", "<kit>", kitName);
            return;
        }

        player.closeInventory();
        sessions.join(player, holder.arena, kit);
    }

    private ItemStack itemFor(IKit kit, FfaArena arena) {
        ItemStack item = kit.getIcon() == null ? new ItemStack(Material.DIAMOND_SWORD) : kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(plugin.getConfig().getString("gui.kit-item.name", "&b<kit>")
                    .replace("<kit>", kit.getName())
                    .replace("<arena>", arena.name())
                    .replace("<playing>", String.valueOf(sessions.playing(arena)))));

            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.kit-item.lore")) {
                lore.add(Text.color(line
                        .replace("<kit>", kit.getName())
                        .replace("<arena>", arena.name())
                        .replace("<playing>", String.valueOf(sessions.playing(arena)))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private int nextSlot(List<Integer> configuredSlots, int index, int size) {
        if (!configuredSlots.isEmpty()) {
            if (index >= configuredSlots.size()) {
                return -1;
            }
            int slot = configuredSlots.get(index);
            return slot >= 0 && slot < size ? slot : -1;
        }
        return index < size ? index : -1;
    }

    private Material material(String name) {
        Material material = Material.matchMaterial(name);
        return material == null ? Material.GRAY_STAINED_GLASS_PANE : material;
    }

    private static final class MenuHolder implements InventoryHolder {
        private final FfaArena arena;
        private final Map<Integer, String> kitsBySlot = new HashMap<>();

        private MenuHolder(FfaArena arena) {
            this.arena = arena;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
