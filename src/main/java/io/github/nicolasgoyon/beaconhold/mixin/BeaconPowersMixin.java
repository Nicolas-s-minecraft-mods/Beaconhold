package io.github.nicolasgoyon.beaconhold.mixin;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconPower;
import io.github.nicolasgoyon.beaconhold.beacon.PowerRules;
import io.github.nicolasgoyon.beaconhold.beacon.VanillaPowerArea;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Changes the vanilla powers of a beacon:
 *
 * <ul>
 *   <li>A beacon that is off (no consumable) gives no power.
 *   <li>The players in the area of the beacon get the powers, in place of the vanilla range. The server option
 *       {@code area.vanillaPowersUseArea} turns this rule off.
 *   <li>Each power has a bonus to its level (server option {@code vanillaPowers.levelBonus}).
 * </ul>
 */
// Mixin calls these methods. Their parameters are the parameters of the changed call, then the parameters of
// applyEffects.
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconPowersMixin {
    @Redirect(
            method = "applyEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;"
                            + "Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private static List<Player> beaconhold$playersInArea(
            Level level,
            Class<Player> type,
            AABB vanillaBox,
            Level sameLevel,
            BlockPos pos,
            int beaconLevel,
            @Nullable Holder<MobEffect> primaryEffect,
            @Nullable Holder<MobEffect> secondaryEffect) {
        if (level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon && BeaconPower.isOff(beacon)) {
            return List.of();
        }
        if (!BeaconholdConfig.SERVER.vanillaPowersUseArea()) {
            return level.getEntitiesOfClass(type, vanillaBox);
        }
        return VanillaPowerArea.playersInArea(level, pos, BeaconholdConfig.SERVER.radius(beaconLevel));
    }

    @ModifyArg(
            method = "applyEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffectInstance;<init>(Lnet/minecraft/core/Holder;IIZZ)V"),
            index = 2)
    private static int beaconhold$levelBonus(int amplifier) {
        return PowerRules.amplifier(amplifier, BeaconholdConfig.SERVER.levelBonus());
    }
}
