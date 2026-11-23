package io.github.nicolasgoyon.beaconhold.network;

import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The areas that the client shows as energy domes: the active beacons of the dimension of the player, with a function
 * that shows its area. The server sends the full list each time that the list changes.
 *
 * @param areas the areas
 */
public record AreaSyncPayload(List<Area> areas) implements CustomPacketPayload {
    /** The type of the payload. */
    public static final Type<AreaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BeaconholdApi.MOD_ID, "areas"));

    /** The maximum number of areas in one payload. */
    public static final int MAX_AREAS = 4096;

    /** The codec of the payload. */
    public static final StreamCodec<ByteBuf, AreaSyncPayload> STREAM_CODEC = Area.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_AREAS))
            .map(AreaSyncPayload::new, AreaSyncPayload::areas);

    /** Makes a payload. */
    public AreaSyncPayload {
        areas = List.copyOf(areas);
    }

    @Override
    public Type<AreaSyncPayload> type() {
        return TYPE;
    }

    /**
     * The area of one active beacon.
     *
     * @param center the position of the beacon
     * @param radius the radius of the area in blocks
     */
    public record Area(BlockPos center, int radius) {
        /** The codec of an area. */
        public static final StreamCodec<ByteBuf, Area> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Area::center, ByteBufCodecs.VAR_INT, Area::radius, Area::new);

        /** Makes an area. */
        public Area {
            center = center.immutable();
        }
    }
}
