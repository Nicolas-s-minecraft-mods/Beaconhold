package io.github.nicolasgoyon.beaconhold.mixin;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Updates the record and the stock of a beacon after the tick of the beacon, at the interval of the vanilla powers.
 */
// Mixin calls the injected method. The parameters must be the same as the parameters of the target method.
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private static void beaconhold$afterTick(
            Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon, CallbackInfo info) {
        if (level instanceof ServerLevel serverLevel
                && level.getGameTime() % BeaconTracker.UPDATE_INTERVAL == 0L) {
            BeaconTracker.update(serverLevel, beacon, true);
        }
    }
}
