package io.github.nicolasgoyon.beaconhold.mixin;

import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives access to the position of the beacon of a beacon menu. */
@Mixin(BeaconMenu.class)
public interface BeaconMenuAccessor {
    /**
     * Gives the access to the beacon of the menu.
     *
     * @return the access. On the client, the access has no position.
     */
    @Accessor("access")
    ContainerLevelAccess getBeaconholdAccess();
}
