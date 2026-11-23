package io.github.nicolasgoyon.beaconhold.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SpawnRulesTest {
    @Test
    void stopsTheNaturalSpawnsOfMonsters() {
        assertTrue(SpawnRules.blocks(MobCategory.MONSTER, MobSpawnType.NATURAL, false));
    }

    @Test
    void stopsTheMonstersAtTheGenerationOfAChunk() {
        assertTrue(SpawnRules.blocks(MobCategory.MONSTER, MobSpawnType.CHUNK_GENERATION, false));
    }

    @Test
    void stopsTheSpawnersOnlyWithTheOption() {
        assertFalse(SpawnRules.blocks(MobCategory.MONSTER, MobSpawnType.SPAWNER, false));
        assertTrue(SpawnRules.blocks(MobCategory.MONSTER, MobSpawnType.SPAWNER, true));
    }

    @Test
    void stopsTheTrialSpawnersOnlyWithTheOption() {
        assertFalse(SpawnRules.blocks(MobCategory.MONSTER, MobSpawnType.TRIAL_SPAWNER, false));
        assertTrue(SpawnRules.blocks(MobCategory.MONSTER, MobSpawnType.TRIAL_SPAWNER, true));
    }

    @ParameterizedTest
    @EnumSource(
            value = MobSpawnType.class,
            names = {"EVENT", "PATROL", "SPAWN_EGG", "COMMAND", "DISPENSER", "BUCKET", "TRIGGERED"})
    void doesNotStopTheRaidsAndTheSpawnsThatAPlayerStarts(MobSpawnType type) {
        assertFalse(SpawnRules.blocks(MobCategory.MONSTER, type, true), type.name());
        assertFalse(SpawnRules.blocks(MobCategory.MONSTER, type, false), type.name());
    }

    @ParameterizedTest
    @EnumSource(
            value = MobSpawnType.class,
            names = {"NATURAL", "CHUNK_GENERATION", "SPAWNER", "TRIAL_SPAWNER"},
            mode = EnumSource.Mode.EXCLUDE)
    void doesNotStopTheOtherSpawnsOfMonsters(MobSpawnType type) {
        assertFalse(SpawnRules.blocks(MobCategory.MONSTER, type, true), type.name());
    }

    @ParameterizedTest
    @EnumSource(value = MobCategory.class, names = "MONSTER", mode = EnumSource.Mode.EXCLUDE)
    void doesNotStopTheMobsThatAreNotHostile(MobCategory category) {
        for (MobSpawnType type : MobSpawnType.values()) {
            assertFalse(SpawnRules.blocks(category, type, true), category.name() + " " + type.name());
        }
    }
}
