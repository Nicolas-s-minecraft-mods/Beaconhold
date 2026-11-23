package io.github.nicolasgoyon.beaconhold.mixin;

import io.github.nicolasgoyon.beaconhold.cost.BeaconStock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** Drops the stock of the consumable when a player, an explosion or a command removes a beacon. */
@Mixin(BeaconBlock.class)
public abstract class BeaconBlockMixin extends BaseEntityBlock {
    protected BeaconBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide()
                && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon) {
            BeaconStock.drop(level, beacon);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
