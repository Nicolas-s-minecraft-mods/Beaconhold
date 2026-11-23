package io.github.nicolasgoyon.beaconhold.function;

import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** The function "no hostile spawns": it stops the spawns of hostile mobs in the area of the beacon. */
public final class NoHostileSpawns {
    private NoHostileSpawns() {
    }

    /** Adds the listener of the spawns to the NeoForge event bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(FinalizeSpawnEvent.class, NoHostileSpawns::onFinalizeSpawn);
    }

    private static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        boolean blockSpawners = BeaconholdConfig.SERVER.blockSpawners();
        if (!SpawnRules.blocks(event.getEntity().getType().getCategory(), event.getSpawnType(), blockSpawners)) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (BeaconholdApi.isCovered(level, pos, ModFunctions.NO_HOSTILE_SPAWNS.getKey())) {
            event.setSpawnCancelled(true);
        }
    }
}
