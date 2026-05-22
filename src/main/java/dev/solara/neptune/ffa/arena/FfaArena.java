package dev.solara.neptune.ffa.arena;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.LinkedHashSet;
import java.util.Set;

public final class FfaArena {
    private final String name;
    private Location pos1;
    private Location pos2;
    private Location spawn;
    private boolean allowPlace = true;
    private boolean allowBreak = true;
    private boolean autoRegen;
    private int menuSlot = -1;
    private Material icon = Material.DIAMOND_SWORD;
    private int spawnSafeRadius = 8;
    private final Set<String> linkedKits = new LinkedHashSet<>();

    public FfaArena(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Location pos1() {
        return pos1;
    }

    public void pos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location pos2() {
        return pos2;
    }

    public void pos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Location spawn() {
        return spawn;
    }

    public void spawn(Location spawn) {
        if (spawn == null) {
            this.spawn = null;
            return;
        }

        Location centered = spawn.clone();
        centered.setX(centered.getBlockX() + 0.5D);
        centered.setZ(centered.getBlockZ() + 0.5D);
        this.spawn = centered;
    }

    public boolean allowPlace() {
        return allowPlace;
    }

    public void allowPlace(boolean allowPlace) {
        this.allowPlace = allowPlace;
    }

    public boolean allowBreak() {
        return allowBreak;
    }

    public void allowBreak(boolean allowBreak) {
        this.allowBreak = allowBreak;
    }

    public boolean autoRegen() {
        return autoRegen;
    }

    public void autoRegen(boolean autoRegen) {
        this.autoRegen = autoRegen;
    }

    public int menuSlot() {
        return menuSlot;
    }

    public void menuSlot(int menuSlot) {
        this.menuSlot = menuSlot;
    }

    public Material icon() {
        return icon;
    }

    public void icon(Material icon) {
        this.icon = icon == null || icon.isAir() ? Material.DIAMOND_SWORD : icon;
    }

    public Set<String> linkedKits() {
        return linkedKits;
    }

    public int spawnSafeRadius() {
        return spawnSafeRadius;
    }

    public void spawnSafeRadius(int spawnSafeRadius) {
        this.spawnSafeRadius = Math.max(0, spawnSafeRadius);
    }

    public boolean isSetup() {
        return pos1 != null && pos2 != null && pos1.getWorld() != null && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public boolean contains(Location location) {
        if (!isSetup() || location.getWorld() == null || !location.getWorld().equals(pos1.getWorld())) {
            return false;
        }

        return location.getX() >= Math.min(pos1.getX(), pos2.getX())
                && location.getX() <= Math.max(pos1.getX(), pos2.getX())
                && location.getY() >= Math.min(pos1.getY(), pos2.getY())
                && location.getY() <= Math.max(pos1.getY(), pos2.getY())
                && location.getZ() >= Math.min(pos1.getZ(), pos2.getZ())
                && location.getZ() <= Math.max(pos1.getZ(), pos2.getZ());
    }

    public boolean inSpawnSafeZone(Location location) {
        if (spawn == null || spawn.getWorld() == null || location == null || location.getWorld() == null
                || !spawn.getWorld().equals(location.getWorld())) {
            return false;
        }

        return Math.abs(location.getBlockX() - spawn.getBlockX()) <= spawnSafeRadius
                && Math.abs(location.getBlockZ() - spawn.getBlockZ()) <= spawnSafeRadius;
    }
}
