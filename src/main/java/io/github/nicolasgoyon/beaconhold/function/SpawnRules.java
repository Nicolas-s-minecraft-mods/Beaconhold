package io.github.nicolasgoyon.beaconhold.function;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;

/** The rule that tells which spawns the function "no hostile spawns" stops. */
public final class SpawnRules {
    private SpawnRules() {
    }

    /**
     * Tells if the function stops a spawn in its area.
     *
     * <p>The function stops only the mobs of the {@code monster} category. It stops the natural spawns and the spawns
     * at the generation of a chunk. It stops the spawns of a monster spawner only when the option is on. It does not
     * stop the other spawns, for example the patrols, the raids and the trial spawners.
     *
     * @param category the category of the mob
     * @param type the cause of the spawn
     * @param blockSpawners the value of the server option {@code noHostileSpawns.blockSpawners}
     * @return true if the function stops the spawn
     */
    public static boolean blocks(MobCategory category, MobSpawnType type, boolean blockSpawners) {
        if (category != MobCategory.MONSTER) {
            return false;
        }
        return switch (type) {
            case NATURAL, CHUNK_GENERATION -> true;
            case SPAWNER -> blockSpawners;
            default -> false;
        };
    }
}
