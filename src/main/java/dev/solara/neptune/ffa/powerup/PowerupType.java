package dev.solara.neptune.ffa.powerup;

import org.bukkit.potion.PotionEffectType;

public enum PowerupType {
    SPEED(PotionEffectType.SPEED),
    STRENGTH(PotionEffectType.STRENGTH),
    REGENERATION(PotionEffectType.REGENERATION),
    JUMP(PotionEffectType.JUMP_BOOST);

    private final PotionEffectType effect;

    PowerupType(PotionEffectType effect) {
        this.effect = effect;
    }

    public PotionEffectType effect() {
        return effect;
    }
}
