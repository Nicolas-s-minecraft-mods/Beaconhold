package io.github.nicolasgoyon.beaconhold.network;

import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The request of a client to open the function screen of the beacon of its open beacon menu. The payload has no
 * position: the server uses the open menu of the player.
 */
public record OpenFunctionsPayload() implements CustomPacketPayload {
    /** The only value of the payload. */
    public static final OpenFunctionsPayload INSTANCE = new OpenFunctionsPayload();

    /** The type of the payload. */
    public static final Type<OpenFunctionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BeaconholdApi.MOD_ID, "open_functions"));

    /** The codec of the payload. */
    public static final StreamCodec<ByteBuf, OpenFunctionsPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenFunctionsPayload> type() {
        return TYPE;
    }
}
