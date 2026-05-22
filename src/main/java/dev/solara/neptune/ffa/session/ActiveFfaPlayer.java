package dev.solara.neptune.ffa.session;

import dev.lrxh.api.kit.IKit;
import dev.solara.neptune.ffa.arena.FfaArena;

public record ActiveFfaPlayer(FfaArena arena, IKit kit) {
}
