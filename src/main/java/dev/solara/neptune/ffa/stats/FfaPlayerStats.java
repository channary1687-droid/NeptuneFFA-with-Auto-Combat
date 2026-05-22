package dev.solara.neptune.ffa.stats;

import java.util.UUID;

public final class FfaPlayerStats {
    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int bestStreak;
    private int currentStreak;
    private int coins;
    private int xp;
    private int level = 1;
    private int prestige;
    private double damageDealt;
    private int hits;
    private int swings;
    private int headshots;
    private int criticalHits;
    private int bountyClaims;
    private int eventsWon;
    private int battlepassXp;
    private int battlepassTier;
    private int seasonPoints;

    public FfaPlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        kills++;
        currentStreak++;
        bestStreak = Math.max(bestStreak, currentStreak);
    }

    public void kills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public int deaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
        currentStreak = 0;
    }

    public void deaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void bestStreak(int bestStreak) {
        this.bestStreak = Math.max(0, bestStreak);
    }

    public int bestStreak() {
        return bestStreak;
    }

    public int currentStreak() {
        return currentStreak;
    }

    public int coins() {
        return coins;
    }

    public void addCoins(int coins) {
        this.coins += Math.max(0, coins);
    }

    public void removeCoins(int coins) {
        this.coins = Math.max(0, this.coins - Math.max(0, coins));
    }

    public int xp() {
        return xp;
    }

    public void xp(int xp) {
        this.xp = xp;
    }

    public int level() {
        return level;
    }

    public void level(int level) {
        this.level = Math.max(1, level);
    }

    public int prestige() {
        return prestige;
    }

    public void prestige(int prestige) {
        this.prestige = Math.max(0, prestige);
    }

    public double damageDealt() {
        return damageDealt;
    }

    public void addDamage(double damage) {
        damageDealt += Math.max(0.0D, damage);
    }

    public void damageDealt(double damageDealt) {
        this.damageDealt = Math.max(0.0D, damageDealt);
    }

    public int hits() {
        return hits;
    }

    public void addHit() {
        hits++;
    }

    public void hits(int hits) {
        this.hits = Math.max(0, hits);
    }

    public int swings() {
        return swings;
    }

    public void addSwing() {
        swings++;
    }

    public void swings(int swings) {
        this.swings = Math.max(0, swings);
    }

    public double accuracy() {
        return swings == 0 ? 0.0D : Math.min(100.0D, (hits * 100.0D) / swings);
    }

    public int headshots() {
        return headshots;
    }

    public void addHeadshot() {
        headshots++;
    }

    public void headshots(int headshots) {
        this.headshots = Math.max(0, headshots);
    }

    public int criticalHits() {
        return criticalHits;
    }

    public void addCriticalHit() {
        criticalHits++;
    }

    public void criticalHits(int criticalHits) {
        this.criticalHits = Math.max(0, criticalHits);
    }

    public int bountyClaims() {
        return bountyClaims;
    }

    public void addBountyClaim() {
        bountyClaims++;
    }

    public void bountyClaims(int bountyClaims) {
        this.bountyClaims = Math.max(0, bountyClaims);
    }

    public int eventsWon() {
        return eventsWon;
    }

    public void addEventWin() {
        eventsWon++;
    }

    public void eventsWon(int eventsWon) {
        this.eventsWon = Math.max(0, eventsWon);
    }

    public int battlepassXp() {
        return battlepassXp;
    }

    public void battlepassXp(int battlepassXp) {
        this.battlepassXp = Math.max(0, battlepassXp);
    }

    public void addBattlepassXp(int amount) {
        battlepassXp += Math.max(0, amount);
    }

    public int battlepassTier() {
        return battlepassTier;
    }

    public void battlepassTier(int battlepassTier) {
        this.battlepassTier = Math.max(0, battlepassTier);
    }

    public int seasonPoints() {
        return seasonPoints;
    }

    public void addSeasonPoints(int amount) {
        seasonPoints += Math.max(0, amount);
    }

    public void seasonPoints(int seasonPoints) {
        this.seasonPoints = Math.max(0, seasonPoints);
    }
}
