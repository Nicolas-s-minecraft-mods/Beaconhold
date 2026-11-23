package io.github.nicolasgoyon.beaconhold.mixin;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconPower;
import io.github.nicolasgoyon.beaconhold.beacon.PowerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes a beacon that is off silent: no ambient sound and no deactivation sound. The vanilla activation sound does not
 * play. The beacon plays it when its power state goes on (see {@link PowerRules#powerSound}).
 */
// Mixin calls these methods. Their parameters are the parameters of the changed call, then the parameters of the
// target method.
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconSoundMixin {
    private static final String PLAY_SOUND = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;playSound("
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;)V";

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = PLAY_SOUND))
    private static void beaconhold$tickSound(
            Level level,
            BlockPos pos,
            SoundEvent sound,
            Level sameLevel,
            BlockPos samePos,
            BlockState state,
            BeaconBlockEntity beacon) {
        if (PowerRules.playsVanillaSound(sound == SoundEvents.BEACON_ACTIVATE, BeaconPower.isOff(beacon))) {
            BeaconBlockEntity.playSound(level, pos, sound);
        }
    }

    @Redirect(method = "setRemoved", at = @At(value = "INVOKE", target = PLAY_SOUND))
    private void beaconhold$removedSound(Level level, BlockPos pos, SoundEvent sound) {
        if (PowerRules.playsVanillaSound(false, BeaconPower.isOff((BeaconBlockEntity) (Object) this))) {
            BeaconBlockEntity.playSound(level, pos, sound);
        }
    }
}
