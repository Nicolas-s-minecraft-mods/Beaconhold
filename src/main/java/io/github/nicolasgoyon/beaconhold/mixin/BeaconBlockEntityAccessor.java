package io.github.nicolasgoyon.beaconhold.mixin;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives access to the pyramid level of a beacon. */
@Mixin(BeaconBlockEntity.class)
public interface BeaconBlockEntityAccessor {
    /**
     * Gives the pyramid level of the beacon.
     *
     * @return the number of complete layers of the pyramid, from 0 to 4
     */
    @Accessor("levels")
    int getBeaconholdLevels();
}
