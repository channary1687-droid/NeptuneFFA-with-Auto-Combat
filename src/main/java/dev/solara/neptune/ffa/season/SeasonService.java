package dev.solara.neptune.ffa.season;

import org.bukkit.plugin.java.JavaPlugin;

public final class SeasonService {
    private final JavaPlugin plugin;

    public SeasonService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String current() {
        return plugin.getConfig().getString("seasons.current", "Season 1");
    }
}
