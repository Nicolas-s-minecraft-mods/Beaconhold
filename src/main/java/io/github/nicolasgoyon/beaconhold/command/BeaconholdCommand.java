package io.github.nicolasgoyon.beaconhold.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconRecord;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The commands of the operators. {@code /beaconhold function} gets, sets and removes the function of a beacon.
 * {@code /beaconhold chunks limit} shows and sets the maximum number of chunks that all the beacons load together.
 */
public final class BeaconholdCommand {
    private static final String PREFIX = "commands.beaconhold.function.";
    private static final String CHUNKS_PREFIX = "commands.beaconhold.chunks.limit.";
    private static final DynamicCommandExceptionType NO_BEACON =
            new DynamicCommandExceptionType(pos -> Component.translatable(PREFIX + "no_beacon", pos));
    private static final DynamicCommandExceptionType UNKNOWN_FUNCTION =
            new DynamicCommandExceptionType(id -> Component.translatable(PREFIX + "unknown", id));

    private BeaconholdCommand() {
    }

    /** Adds the listener that registers the command to the NeoForge event bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> register(event.getDispatcher()));
    }

    /**
     * Registers the command.
     *
     * @param dispatcher the dispatcher of the commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(BeaconholdApi.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("function")
                        .then(Commands.literal("get")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(BeaconholdCommand::get)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("function", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        ModRegistries.FUNCTIONS.keySet(), builder))
                                                .executes(BeaconholdCommand::set))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(BeaconholdCommand::clear))))
                .then(Commands.literal("chunks")
                        .then(Commands.literal("limit")
                                .executes(BeaconholdCommand::getLimit)
                                .then(Commands.argument(
                                                "chunks",
                                                IntegerArgumentType.integer(0, BeaconholdConfig.MAX_LOADED_CHUNKS))
                                        .executes(BeaconholdCommand::setLimit)))));
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        requireBeacon(level, pos);
        Optional<BeaconRecord> beacon = BeaconTracker.getRecord(level, pos);
        Component message = beacon.map(record -> describe(pos, record))
                .orElseGet(() -> Component.translatable(PREFIX + "get.none", format(pos)));
        context.getSource().sendSuccess(() -> message, false);
        return beacon.isPresent() ? 1 : 0;
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ResourceLocation id = ResourceLocationArgument.getId(context, "function");
        if (!ModRegistries.FUNCTIONS.containsKey(id)) {
            throw UNKNOWN_FUNCTION.create(id.toString());
        }
        ResourceKey<BeaconFunction> function = ResourceKey.create(BeaconholdApi.FUNCTION_REGISTRY_KEY, id);
        if (!BeaconholdApi.setFunction(level, pos, function)) {
            throw NO_BEACON.create(format(pos));
        }
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(PREFIX + "set", format(pos), BeaconFunction.name(function)),
                        true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        requireBeacon(level, pos);
        BeaconholdApi.setFunction(level, pos, null);
        context.getSource().sendSuccess(() -> Component.translatable(PREFIX + "clear", format(pos)), true);
        return 1;
    }

    private static int getLimit(CommandContext<CommandSourceStack> context) {
        int used = LoadedChunks.totalLoadedChunks(context.getSource().getLevel());
        int limit = BeaconholdConfig.SERVER.maxLoadedChunks();
        context.getSource().sendSuccess(() -> Component.translatable(CHUNKS_PREFIX + "get", used, limit), false);
        return limit;
    }

    private static int setLimit(CommandContext<CommandSourceStack> context) {
        int limit = IntegerArgumentType.getInteger(context, "chunks");
        BeaconholdConfig.SERVER.setMaxLoadedChunks(limit);
        BeaconholdConfig.SERVER_SPEC.save();
        int used = LoadedChunks.totalLoadedChunks(context.getSource().getLevel());
        context.getSource().sendSuccess(() -> Component.translatable(CHUNKS_PREFIX + "set", limit, used), true);
        return limit;
    }

    private static void requireBeacon(ServerLevel level, BlockPos pos) throws CommandSyntaxException {
        if (!(level.getBlockEntity(pos) instanceof BeaconBlockEntity)) {
            throw NO_BEACON.create(format(pos));
        }
    }

    private static Component describe(BlockPos pos, BeaconRecord beacon) {
        Component name = BeaconFunction.name(ResourceKey.create(BeaconholdApi.FUNCTION_REGISTRY_KEY, beacon.function()));
        if (beacon.active()) {
            int radius = BeaconholdConfig.SERVER.radius(beacon.pyramidLevel());
            return Component.translatable(PREFIX + "get.active", format(pos), name, beacon.pyramidLevel(), radius);
        }
        return Component.translatable(PREFIX + "get.inactive", format(pos), name);
    }

    private static String format(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
