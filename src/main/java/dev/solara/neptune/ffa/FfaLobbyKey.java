package dev.solara.neptune.ffa;

public record FfaLobbyKey(String kitName, String arenaName) {
    public String id() {
        return kitName.toLowerCase() + ":" + arenaName.toLowerCase();
    }
}
