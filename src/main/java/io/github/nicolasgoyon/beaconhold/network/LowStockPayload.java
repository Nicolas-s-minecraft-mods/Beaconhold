package io.github.nicolasgoyon.beaconhold.network;

import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server tells a player in the area of an active beacon that the stock of the beacon is low.
 *
 * @param pos the position of the beacon
 * @param remainingTicks the time of operation that the stock and the fuel give, in ticks
 */
public record LowStockPayload(BlockPos pos, int remainingTicks) implements CustomPacketPayload {
    /** The type of the payload. */
    public static final Type<LowStockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BeaconholdApi.MOD_ID, "low_stock"));

    /** The codec of the payload. */
    public static final StreamCodec<ByteBuf, LowStockPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            LowStockPayload::pos,
            ByteBufCodecs.VAR_INT,
            LowStockPayload::remainingTicks,
            LowStockPayload::new);

    /** Makes a payload. */
    public LowStockPayload {
        pos = pos.immutable();
    }

    @Override
    public Type<LowStockPayload> type() {
        return TYPE;
    }
}
