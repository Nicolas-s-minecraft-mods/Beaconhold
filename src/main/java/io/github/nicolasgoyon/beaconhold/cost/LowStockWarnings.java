package io.github.nicolasgoyon.beaconhold.cost;

import io.github.nicolasgoyon.beaconhold.beacon.VanillaPowerArea;
import io.github.nicolasgoyon.beaconhold.network.LowStockPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends the warning for a low stock to the players in the area of an active beacon. Each update of a beacon with less
 * than one game day of stock and fuel sends the warning. The client decides if it shows the warning (see
 * {@link WarningRules#shouldWarn}).
 */
public final class LowStockWarnings {
    /** For each dimension: the last warning of each beacon. The server thread uses this map. */
    private static final Map<ServerLevel, Map<BlockPos, Warning>> LAST = new WeakHashMap<>();

    private LowStockWarnings() {
    }

    /**
     * Sends the warning to the players in the area of a beacon.
     *
     * @param level the dimension of the beacon
     * @param pos the position of the beacon
     * @param radius the radius of the area in blocks
     * @param remainingTicks the time of operation that the stock and the fuel give, in ticks
     */
    public static void send(ServerLevel level, BlockPos pos, int radius, long remainingTicks) {
        List<Player> players = VanillaPowerArea.playersInArea(level, pos, radius);
        LowStockPayload payload = new LowStockPayload(pos, (int) Math.min(Integer.MAX_VALUE, remainingTicks));
        for (Player player : players) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, payload);
            }
        }
        LAST.computeIfAbsent(level, key -> new HashMap<>())
                .put(pos.immutable(), new Warning(level.getGameTime(), players.size()));
    }

    /**
     * Removes the last warning of a beacon that a player or an explosion removes.
     *
     * @param level the dimension of the beacon
     * @param pos the position of the beacon
     */
    public static void remove(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Warning> warnings = LAST.get(level);
        if (warnings != null) {
            warnings.remove(pos);
        }
    }

    /**
     * Gives the last warning of a beacon.
     *
     * @param level the dimension of the beacon
     * @param pos the position of the beacon
     * @return the warning, or {@code null} if the beacon sent no warning
     */
    @Nullable
    public static Warning last(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Warning> warnings = LAST.get(level);
        return warnings == null ? null : warnings.get(pos);
    }

    /**
     * A warning that a beacon sent.
     *
     * @param gameTime the game time of the warning
     * @param players the number of players in the area
     */
    public record Warning(long gameTime, int players) {
    }
}
