package dev.solara.neptune.ffa.util;

import dev.lrxh.api.kit.IKit;
import dev.lrxh.api.kit.IKitRule;

import java.util.Map;

public final class KitRules {
    private KitRules() {
    }

    public static boolean enabled(IKit kit, String saveName) {
        for (Map.Entry<IKitRule, Boolean> entry : kit.getRule().entrySet()) {
            if (entry.getKey().getSaveName().equalsIgnoreCase(saveName)) {
                return Boolean.TRUE.equals(entry.getValue());
            }
        }
        return false;
    }
}
