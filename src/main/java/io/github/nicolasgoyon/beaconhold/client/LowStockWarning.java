package io.github.nicolasgoyon.beaconhold.client;

import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.cost.CostRules;
import io.github.nicolasgoyon.beaconhold.cost.WarningRules;
import io.github.nicolasgoyon.beaconhold.network.LowStockPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Shows the warning for a low stock of a beacon: a message above the hotbar and a sound. The client shows the warning of
 * a beacon at most one time in {@link WarningRules#COOLDOWN_TICKS} ticks. The client option {@code warnings.lowStock}
 * turns the warning off.
 */
public final class LowStockWarning {
    /** The client tick of the last warning of each beacon. The client thread uses this map. */
    private static final Map<BlockPos, Long> LAST = new HashMap<>();

    private LowStockWarning() {
    }

    /**
     * Shows a warning from the server, if the option and the cooldown let it.
     *
     * @param payload the warning
     */
    public static void show(LowStockPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!BeaconholdConfig.CLIENT.lowStockWarning() || minecraft.level == null || minecraft.player == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (!WarningRules.shouldWarn(LAST.get(payload.pos()), now)) {
            return;
        }
        LAST.put(payload.pos(), now);
        minecraft.gui.setOverlayMessage(
                Component.translatable("hud.beaconhold.low_stock", CostRules.days(payload.remainingTicks()))
                        .withStyle(ChatFormatting.RED),
                false);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 0.6F));
    }

    /** Removes the warnings of the last server. */
    public static void clear() {
        LAST.clear();
    }
}
