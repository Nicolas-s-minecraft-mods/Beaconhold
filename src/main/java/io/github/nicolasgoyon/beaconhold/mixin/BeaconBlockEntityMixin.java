package io.github.nicolasgoyon.beaconhold.mixin;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconLoadRules;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconLoadState;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.beacon.BeamBeacons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the load state of a beacon (see {@link BeaconLoadRules}). After the tick of the beacon, at the interval of the
 * vanilla powers, it updates the record and the stock of the beacon, and the list of the beacons that show the dome.
 */
// Mixin calls the injected method. The parameters must be the same as the parameters of the target method.
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin implements BeaconLoadState {
    @Shadow
    private int lastCheckY;

    @Unique
    private int beaconhold$loadState = BeaconLoadRules.LOADED;

    @Override
    public int beaconhold$loadState() {
        return beaconhold$loadState;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void beaconhold$afterTick(
            Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon, CallbackInfo info) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BeaconBlockEntityMixin self = (BeaconBlockEntityMixin) (Object) beacon;
        boolean periodic = level.getGameTime() % BeaconTracker.UPDATE_INTERVAL == 0L;
        // The tick sets this value only at the end of a scan of the beam.
        boolean scanCompleted = self.lastCheckY == level.getMinBuildHeight() - 1;
        self.beaconhold$loadState = BeaconLoadRules.afterTick(self.beaconhold$loadState, periodic, scanCompleted);
        if (periodic) {
            BeaconTracker.update(serverLevel, beacon, true);
            if (self.beaconhold$loadState == BeaconLoadRules.READY) {
                BeamBeacons.update(serverLevel, beacon);
            }
        }
    }
}
