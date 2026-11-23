package io.github.nicolasgoyon.beaconhold.beacon;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * The saved state of a beacon with a function.
 *
 * @param pos the position of the beacon
 * @param pyramidLevel the pyramid level, from 0 to 4
 * @param active true if the function is active
 * @param function the id of the function
 * @param owner the player that placed the beacon, or {@code null} if no player is known
 */
public record BeaconRecord(
        BlockPos pos, int pyramidLevel, boolean active, ResourceLocation function, @Nullable UUID owner) {
    private static final String TAG_POS = "pos";
    private static final String TAG_LEVEL = "pyramid_level";
    private static final String TAG_ACTIVE = "active";
    private static final String TAG_FUNCTION = "function";
    private static final String TAG_OWNER = "owner";

    /** Makes a record. */
    public BeaconRecord {
        pos = pos.immutable();
    }

    /**
     * Makes a record of a beacon with no known owner.
     *
     * @param pos the position of the beacon
     * @param pyramidLevel the pyramid level, from 0 to 4
     * @param active true if the function is active
     * @param function the id of the function
     */
    public BeaconRecord(BlockPos pos, int pyramidLevel, boolean active, ResourceLocation function) {
        this(pos, pyramidLevel, active, function, null);
    }

    /**
     * Tells if the active function of this record and of another record are the same.
     *
     * @param other the other record
     * @return true if the two records are active, with the same function and the same pyramid level
     */
    public boolean sameActiveFunction(BeaconRecord other) {
        return active
                && other.active
                && pyramidLevel == other.pyramidLevel
                && function.equals(other.function)
                && pos.equals(other.pos);
    }

    /**
     * Writes this record to a tag.
     *
     * @return a new tag
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(TAG_POS, pos.asLong());
        tag.putInt(TAG_LEVEL, pyramidLevel);
        tag.putBoolean(TAG_ACTIVE, active);
        tag.putString(TAG_FUNCTION, function.toString());
        if (owner != null) {
            tag.putUUID(TAG_OWNER, owner);
        }
        return tag;
    }

    /**
     * Reads a record from a tag.
     *
     * @param tag the tag
     * @return the record, or {@code null} if the tag has no valid function id
     */
    @Nullable
    public static BeaconRecord load(CompoundTag tag) {
        ResourceLocation function = ResourceLocation.tryParse(tag.getString(TAG_FUNCTION));
        if (function == null) {
            return null;
        }
        return new BeaconRecord(
                BlockPos.of(tag.getLong(TAG_POS)),
                tag.getInt(TAG_LEVEL),
                tag.getBoolean(TAG_ACTIVE),
                function,
                tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null);
    }
}
