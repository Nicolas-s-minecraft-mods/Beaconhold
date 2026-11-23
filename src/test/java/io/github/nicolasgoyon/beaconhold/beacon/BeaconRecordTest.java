package io.github.nicolasgoyon.beaconhold.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
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
    void saveThenLoadKeepsTheOwner() {
        UUID owner = UUID.fromString("0f6a1c5e-3b2d-4c8e-9a71-5d2e8b4f6c10");
        BeaconRecord beacon = new BeaconRecord(
                new BlockPos(5, 70, -9), 4, true, ResourceLocation.fromNamespaceAndPath("beaconhold", "x"), owner);
        BeaconRecord loaded = BeaconRecord.load(beacon.save());
        assertEquals(beacon, loaded);
        assertEquals(owner, loaded.owner());
    }

    @Test
    void recordWithNoOwnerLoadsWithNoOwner() {
        BeaconRecord beacon =
                new BeaconRecord(BlockPos.ZERO, 1, true, ResourceLocation.fromNamespaceAndPath("beaconhold", "x"));
        assertNull(BeaconRecord.load(beacon.save()).owner());
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
