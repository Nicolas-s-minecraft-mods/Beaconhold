package io.github.nicolasgoyon.beaconhold.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockItemHandlerTest {
    private BeaconBlockEntity beacon;
    private StockItemHandler handler;

    @BeforeEach
    void makeBeacon() {
        beacon = new BeaconBlockEntity(BlockPos.ZERO, Blocks.BEACON.defaultBlockState());
        handler = new StockItemHandler(
                beacon, stack -> stack.is(Items.QUARTZ) || stack.is(Items.AMETHYST_SHARD));
    }

    @Test
    void consumableGoesIntoAnEmptyStock() {
        ItemStack rest = handler.insertItem(0, new ItemStack(Items.QUARTZ, 5), false);
        assertTrue(rest.isEmpty());
        assertEquals(5, BeaconStock.stock(beacon).getCount());
        assertTrue(BeaconStock.stock(beacon).is(Items.QUARTZ));
    }

    @Test
    void simulationChangesNothing() {
        ItemStack rest = handler.insertItem(0, new ItemStack(Items.QUARTZ, 5), true);
        assertTrue(rest.isEmpty());
        assertTrue(BeaconStock.stock(beacon).isEmpty());
    }

    @Test
    void stockHoldsOneStackOfOneItem() {
        BeaconStock.setStock(beacon, new ItemStack(Items.QUARTZ, 60));
        ItemStack rest = handler.insertItem(0, new ItemStack(Items.QUARTZ, 10), false);
        assertEquals(6, rest.getCount());
        assertEquals(64, BeaconStock.stock(beacon).getCount());

        ItemStack other = new ItemStack(Items.AMETHYST_SHARD, 3);
        BeaconStock.setStock(beacon, new ItemStack(Items.QUARTZ, 1));
        assertSame(other, handler.insertItem(0, other, false), "A second item type stays out");
        assertEquals(1, BeaconStock.stock(beacon).getCount());
    }

    @Test
    void otherItemsAndOtherSlotsStayOut() {
        ItemStack dirt = new ItemStack(Items.DIRT, 4);
        assertSame(dirt, handler.insertItem(0, dirt, false));
        ItemStack quartz = new ItemStack(Items.QUARTZ, 4);
        assertSame(quartz, handler.insertItem(1, quartz, false));
        assertTrue(BeaconStock.stock(beacon).isEmpty());
    }

    @Test
    void nothingComesOut() {
        BeaconStock.setStock(beacon, new ItemStack(Items.QUARTZ, 20));
        assertTrue(handler.extractItem(0, 64, false).isEmpty());
        assertTrue(handler.extractItem(0, 64, true).isEmpty());
        assertEquals(20, BeaconStock.stock(beacon).getCount());
        assertEquals(1, handler.getSlots());
        assertEquals(20, handler.getStackInSlot(0).getCount());
    }
}
