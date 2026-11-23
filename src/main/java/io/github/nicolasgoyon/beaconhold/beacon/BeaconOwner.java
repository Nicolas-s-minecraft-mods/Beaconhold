package io.github.nicolasgoyon.beaconhold.beacon;

import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The owner of a beacon: the player that placed it. The beacon saves the owner. A beacon that no player placed, for
 * example a beacon from a command or a structure, has no owner. The limit of the loaded chunks for each player uses the
 * owner.
 */
public final class BeaconOwner {
    private BeaconOwner() {
    }

    /** Adds the listener that saves the owner when a player places a beacon. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, BeaconOwner::onPlace);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player
                && !event.getLevel().isClientSide()
                && event.getLevel().getBlockEntity(event.getPos()) instanceof BeaconBlockEntity beacon) {
            set(beacon, player.getUUID());
        }
    }

    /**
     * Gives the owner of a beacon.
     *
     * @param beacon the beacon
     * @return the UUID of the player that placed the beacon, or {@code null} if no player is known
     */
    @Nullable
    public static UUID get(BeaconBlockEntity beacon) {
        return beacon.getExistingData(ModRegistries.BEACON_OWNER).orElse(null);
    }

    /**
     * Sets the owner of a beacon.
     *
     * @param beacon the beacon
     * @param owner the UUID of the player
     */
    public static void set(BeaconBlockEntity beacon, UUID owner) {
        beacon.setData(ModRegistries.BEACON_OWNER, owner);
        beacon.setChanged();
    }
}
