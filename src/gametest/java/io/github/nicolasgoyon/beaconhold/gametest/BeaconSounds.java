package io.github.nicolasgoyon.beaconhold.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/** A record of the beacon sounds that the server plays. Only the GameTests use it. */
final class BeaconSounds {
    private static final List<SoundEvent> BEACON_SOUNDS = List.of(
            SoundEvents.BEACON_AMBIENT, SoundEvents.BEACON_ACTIVATE, SoundEvents.BEACON_DEACTIVATE);

    private static final List<Played> PLAYED = new ArrayList<>();

    static {
        NeoForge.EVENT_BUS.addListener(PlayLevelSoundEvent.AtPosition.class, BeaconSounds::record);
    }

    private BeaconSounds() {
    }

    /** A beacon sound at a block position. */
    private record Played(Level level, BlockPos pos, SoundEvent sound) {
    }

    private static synchronized void record(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null || event.getLevel().isClientSide()) {
            return;
        }
        SoundEvent sound = event.getSound().value();
        if (BEACON_SOUNDS.contains(sound)) {
            PLAYED.add(new Played(event.getLevel(), BlockPos.containing(event.getPosition()), sound));
        }
    }

    /**
     * Starts the record of the beacon sounds. Call this method before the sounds of a test.
     *
     * @param level the dimension
     * @param pos the absolute position of the beacon
     */
    static synchronized void clear(Level level, BlockPos pos) {
        PLAYED.removeIf(played -> played.level() == level && played.pos().equals(pos));
    }

    /**
     * Counts the plays of a sound at a position since the last call of {@link #clear}.
     *
     * @param level the dimension
     * @param pos the absolute position of the beacon
     * @param sound the sound
     * @return the number of plays
     */
    static synchronized long count(Level level, BlockPos pos, SoundEvent sound) {
        return PLAYED.stream()
                .filter(played -> played.level() == level && played.pos().equals(pos) && played.sound() == sound)
                .count();
    }

    /**
     * Counts the plays of all the beacon sounds at a position since the last call of {@link #clear}.
     *
     * @param level the dimension
     * @param pos the absolute position of the beacon
     * @return the number of plays
     */
    static synchronized long countAll(Level level, BlockPos pos) {
        return PLAYED.stream()
                .filter(played -> played.level() == level && played.pos().equals(pos))
                .count();
    }
}
