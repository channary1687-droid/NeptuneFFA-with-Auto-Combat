package dev.solara.neptune.ffa.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FfaArenaService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, FfaArena> arenas = new LinkedHashMap<>();

    public FfaArenaService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public synchronized void load() {
        arenas.clear();
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) {
            return;
        }

        for (String name : section.getKeys(false)) {
            FfaArena arena = new FfaArena(name);
            arena.pos1(config.getLocation("arenas." + name + ".pos1"));
            arena.pos2(config.getLocation("arenas." + name + ".pos2"));
            arena.spawn(config.getLocation("arenas." + name + ".spawn"));
            arena.allowPlace(config.getBoolean("arenas." + name + ".allow-place", true));
            arena.allowBreak(config.getBoolean("arenas." + name + ".allow-break", true));
            arena.autoRegen(config.getBoolean("arenas." + name + ".auto-regen", false));
            arena.menuSlot(config.getInt("arenas." + name + ".menu-slot", -1));
            arena.icon(Material.matchMaterial(config.getString("arenas." + name + ".icon", "DIAMOND_SWORD")));
            arena.spawnSafeRadius(config.getInt("arenas." + name + ".spawn-safe-radius", 8));
            arena.linkedKits().addAll(config.getStringList("arenas." + name + ".linked-kits"));
            arenas.put(name.toLowerCase(), arena);
        }
    }

    public synchronized void save() {
        FileConfiguration config = new YamlConfiguration();
        for (FfaArena arena : new ArrayList<>(arenas.values())) {
            String path = "arenas." + arena.name() + ".";
            config.set(path + "pos1", arena.pos1());
            config.set(path + "pos2", arena.pos2());
            config.set(path + "spawn", arena.spawn());
            config.set(path + "allow-place", arena.allowPlace());
            config.set(path + "allow-break", arena.allowBreak());
            config.set(path + "auto-regen", arena.autoRegen());
            config.set(path + "menu-slot", arena.menuSlot());
            config.set(path + "icon", arena.icon().name());
            config.set(path + "spawn-safe-radius", arena.spawnSafeRadius());
            config.set(path + "linked-kits", arena.linkedKits().stream().toList());
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save arenas.yml");
            exception.printStackTrace();
        }
    }

    public synchronized FfaArena create(String name) {
        FfaArena arena = new FfaArena(name);
        arenas.put(name.toLowerCase(), arena);
        save();
        return arena;
    }

    public synchronized boolean delete(String name) {
        FfaArena removed = arenas.remove(name.toLowerCase());
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public synchronized FfaArena get(String name) {
        return arenas.get(name.toLowerCase());
    }

    public synchronized Collection<FfaArena> all() {
        return new ArrayList<>(arenas.values());
    }

    public synchronized FfaArena defaultArena(String configuredName) {
        if (configuredName != null && !configuredName.isBlank()) {
            FfaArena configured = get(configuredName);
            if (configured != null) {
                return configured;
            }
        }
        return arenas.values().stream().findFirst().orElse(null);
    }

    public FfaArena byLocation(Location location) {
        for (FfaArena arena : all()) {
            if (arena.contains(location)) {
                return arena;
            }
        }
        return null;
    }
}
