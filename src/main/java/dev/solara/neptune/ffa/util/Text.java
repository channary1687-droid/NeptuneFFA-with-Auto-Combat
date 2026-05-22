package dev.solara.neptune.ffa.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Text {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    public static void send(CommandSender sender, JavaPlugin plugin, String key, String... placeholders) {
        String message = plugin.getConfig().getString("messages." + key, "");
        if (message == null || message.isEmpty()) return;
        
        raw(sender, plugin, replace(message, placeholders));
    }

    public static void raw(CommandSender sender, JavaPlugin plugin, String message) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        sender.sendMessage(parse(prefix + message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    public static Component parse(String message) {
        if (message == null) return Component.empty();
        return MINI_MESSAGE.deserialize(message.replace("§", "&"));
    }

    private static String replace(String message, String... placeholders) {
        String result = message;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String target = placeholders[i];
            String value = placeholders[i + 1];
            result = result.replace(target, value);
        }
        return result;
    }

    public static String color(String message) {
        if (message == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String colorize(String message) {
        return color(message);
    }
}
