package io.github.nicolasgoyon.beaconhold.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class AreaSyncPayloadTest {
    @Test
    void codecKeepsTheAreas() {
        AreaSyncPayload payload = new AreaSyncPayload(List.of(
                new AreaSyncPayload.Area(new BlockPos(10, 64, -20), 20),
                new AreaSyncPayload.Area(new BlockPos(-3000, -10, 5000), 100)));
        ByteBuf buffer = Unpooled.buffer();
        AreaSyncPayload.STREAM_CODEC.encode(buffer, payload);
        assertEquals(payload, AreaSyncPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void emptyListIsValid() {
        ByteBuf buffer = Unpooled.buffer();
        AreaSyncPayload.STREAM_CODEC.encode(buffer, new AreaSyncPayload(List.of()));
        assertEquals(List.of(), AreaSyncPayload.STREAM_CODEC.decode(buffer).areas());
    }

    @Test
    void payloadCannotChange() {
        List<AreaSyncPayload.Area> areas = new ArrayList<>();
        areas.add(new AreaSyncPayload.Area(BlockPos.ZERO, 20));
        AreaSyncPayload payload = new AreaSyncPayload(areas);
        areas.clear();
        assertEquals(1, payload.areas().size());
        assertThrows(UnsupportedOperationException.class, () -> payload.areas().clear());
    }
}
