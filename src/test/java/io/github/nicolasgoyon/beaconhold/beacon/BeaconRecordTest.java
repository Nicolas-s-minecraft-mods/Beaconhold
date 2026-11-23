package io.github.nicolasgoyon.beaconhold.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BeaconRecordTest {
    @Test
    void saveThenLoadGivesTheSameRecord() {
        BeaconRecord beacon = new BeaconRecord(
                new BlockPos(-120, -40, 3000), 3, true, ResourceLocation.fromNamespaceAndPath("othermod", "shield"));
        assertEquals(beacon, BeaconRecord.load(beacon.save()));
    }

    @Test
    void loadIgnoresATagWithAnInvalidFunction() {
        CompoundTag tag = new BeaconRecord(
                        BlockPos.ZERO, 1, false, ResourceLocation.fromNamespaceAndPath("beaconhold", "a"))
                .save();
        tag.putString("function", "Not A Valid Id");
        assertNull(BeaconRecord.load(tag));
    }
}
