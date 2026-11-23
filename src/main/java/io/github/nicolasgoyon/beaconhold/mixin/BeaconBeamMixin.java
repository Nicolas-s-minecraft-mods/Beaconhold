package io.github.nicolasgoyon.beaconhold.mixin;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconPower;
import java.util.List;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides the beam of a beacon that is off (no consumable) on the client. The server keeps the beam sections, so that the
 * beacon knows if its beam is free.
 */
// Mixin calls the injected method.
@SuppressWarnings("UnusedMethod")
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBeamMixin {
    @Inject(method = "getBeamSections", at = @At("HEAD"), cancellable = true)
    private void beaconhold$hideBeam(CallbackInfoReturnable<List<BeaconBlockEntity.BeaconBeamSection>> info) {
        BeaconBlockEntity self = (BeaconBlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide() && BeaconPower.isOff(self)) {
            info.setReturnValue(List.of());
        }
    }
}
