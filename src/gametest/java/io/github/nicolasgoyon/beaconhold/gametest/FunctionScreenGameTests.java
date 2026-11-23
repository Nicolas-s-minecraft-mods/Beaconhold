package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.function.ChunkRules;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import io.github.nicolasgoyon.beaconhold.menu.BeaconFunctionMenu;
import io.github.nicolasgoyon.beaconhold.menu.FunctionButtons;
import io.github.nicolasgoyon.beaconhold.menu.FunctionList;
import io.github.nicolasgoyon.beaconhold.menu.ModMenus;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests of the function screen of a beacon: the selection of one function, the state and the cost. */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FunctionScreenGameTests {
    private static final int TIMEOUT = 300;

    private FunctionScreenGameTests() {
    }

    private static int buttonOf(ResourceKey<BeaconFunction> function) {
        return FunctionButtons.buttonOf(FunctionList.sorted().indexOf(function));
    }

    private static Player playerAt(GameTestHelper helper, BlockPos relative) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(relative);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }

    /** The menu gives the loaded chunks of the beacon and of the server, and the limit of the server. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_screen_chunks")
    public static void menuShowsTheLoadedChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        Player player = playerAt(helper, BeaconTestSupport.BEACON.east(2));
        BeaconFunctionMenu menu = BeaconFunctionMenu.server(0, player.getInventory(), level, beacon, 3750);
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        int before = config.maxLoadedChunks();
        int area = ChunkRules.areaChunks(beacon.getX(), beacon.getZ(), config.radius(1)).size();
        helper.assertFalse(menu.hasLoadedChunks(), "No function: the screen shows no chunks");
        BeaconholdApi.setFunction(level, beacon, ModFunctions.LOADED_CHUNKS.getKey());
        helper.assertTrue(menu.hasLoadedChunks(), "The function is \"loaded chunks\"");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(menu.loadedChunks() == area, "The beacon must load its area"))
                .thenExecute(() -> {
                    helper.assertTrue(menu.areaChunks() == area, "The menu gives the chunks of the area");
                    helper.assertTrue(
                            menu.serverChunks() == LoadedChunks.totalLoadedChunks(level),
                            "The menu gives the chunks of the server");
                    helper.assertTrue(menu.serverLimit() == before, "The menu gives the limit");
                    config.setMaxLoadedChunks(100_000);
                    helper.assertTrue(menu.serverLimit() == 100_000, "The menu gives a limit above 32767");
                    config.setMaxLoadedChunks(before);
                    BeaconTestSupport.removeBeacon(helper);
                    helper.assertTrue(menu.loadedChunks() == 0, "A removed beacon loads no chunk");
                    helper.assertTrue(menu.areaChunks() == 0, "A removed beacon has no area");
                })
                .thenSucceed();
    }

    /** A click on a function selects it. A new selection replaces the function. The button "none" removes it. */
    @GameTest(template = "beacon", batch = "beaconhold_screen_select")
    public static void clickSelectsOneFunction(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        Player player = playerAt(helper, BeaconTestSupport.BEACON.east(2));
        BeaconFunctionMenu menu = BeaconFunctionMenu.server(0, player.getInventory(), level, beacon, 3750);
        helper.assertTrue(menu.functionIndex() == -1, "No function at the start");

        helper.assertTrue(menu.clickMenuButton(player, buttonOf(ModFunctions.NO_HOSTILE_SPAWNS.getKey())), "Click");
        helper.assertTrue(
                BeaconholdApi.getFunction(level, beacon).equals(Optional.of(ModFunctions.NO_HOSTILE_SPAWNS.getKey())),
                "The click selects the function");
        helper.assertTrue(
                menu.functionIndex() == FunctionList.sorted().indexOf(ModFunctions.NO_HOSTILE_SPAWNS.getKey()),
                "The menu shows the function");

        menu.clickMenuButton(player, buttonOf(ModFunctions.LOADED_CHUNKS.getKey()));
        helper.assertTrue(
                BeaconholdApi.getFunction(level, beacon).equals(Optional.of(ModFunctions.LOADED_CHUNKS.getKey())),
                "A new selection replaces the function");

        menu.clickMenuButton(player, FunctionButtons.NONE);
        helper.assertTrue(BeaconholdApi.getFunction(level, beacon).isEmpty(), "The button none removes the function");
        helper.assertTrue(menu.functionIndex() == -1, "The menu shows no function");
        helper.assertTrue(menu.ticksPerItem() == 3750, "The menu gives the cost");
        BeaconTestSupport.removeBeacon(helper);
        helper.succeed();
    }

    /** A button that is not in the list changes nothing. */
    @GameTest(template = "beacon", batch = "beaconhold_screen_invalid")
    public static void invalidButtonChangesNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        Player player = playerAt(helper, BeaconTestSupport.BEACON.east(2));
        BeaconholdApi.setFunction(level, beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey());
        BeaconFunctionMenu menu = BeaconFunctionMenu.server(0, player.getInventory(), level, beacon, 3750);
        int outside = FunctionButtons.buttonOf(FunctionList.sorted().size());
        helper.assertFalse(menu.clickMenuButton(player, outside), "The server refuses the button");
        helper.assertFalse(menu.clickMenuButton(player, -5), "The server refuses a negative button");
        helper.assertTrue(
                BeaconholdApi.getFunction(level, beacon).equals(Optional.of(ModFunctions.NO_HOSTILE_SPAWNS.getKey())),
                "The beacon keeps its function");
        BeaconTestSupport.removeBeacon(helper);
        helper.succeed();
    }

    /** The menu shows the state of the function: active with a pyramid and a free beam, not active with no pyramid. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_screen_state")
    public static void menuShowsTheState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        Player player = playerAt(helper, BeaconTestSupport.BEACON.east(2));
        BeaconFunctionMenu menu = BeaconFunctionMenu.server(0, player.getInventory(), level, beacon, 3750);
        menu.clickMenuButton(player, buttonOf(TestFunctions.TEST));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(menu.isActive(), "The function must be active"))
                .thenExecute(() -> helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.STONE))
                .thenWaitUntil(() -> helper.assertFalse(menu.isActive(), "A blocked beam: not active"))
                .thenExecute(() -> BeaconTestSupport.removeBeacon(helper))
                .thenSucceed();
    }

    /** The menu closes when the beacon is removed or when the player is far. */
    @GameTest(template = "beacon", batch = "beaconhold_screen_valid")
    public static void menuNeedsTheBeaconNear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        Player near = playerAt(helper, BeaconTestSupport.BEACON.east(2));
        BeaconFunctionMenu menu = BeaconFunctionMenu.server(0, near.getInventory(), level, beacon, 3750);
        helper.assertTrue(menu.stillValid(near), "A player near the beacon");
        Player far = playerAt(helper, BeaconTestSupport.BEACON.east(2));
        far.setPos(beacon.getX() + 20.5, beacon.getY(), beacon.getZ() + 0.5);
        helper.assertFalse(menu.stillValid(far), "A player far from the beacon");
        BeaconTestSupport.removeBeacon(helper);
        helper.assertFalse(menu.stillValid(near), "No beacon: the menu closes");
        helper.succeed();
    }

    /** The server finds the beacon of the open beacon menu of a player. A player with no beacon menu gets nothing. */
    @GameTest(template = "beacon", batch = "beaconhold_screen_open")
    public static void openUsesTheBeaconMenu(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        Player player = playerAt(helper, BeaconTestSupport.BEACON.east(2));
        helper.assertTrue(ModMenus.openBeacon(player).isEmpty(), "No beacon menu: no beacon");
        helper.assertFalse(ModMenus.openFunctions(player), "No beacon menu: no function screen");
        player.containerMenu = new BeaconMenu(
                1, player.getInventory(), new SimpleContainerData(3), ContainerLevelAccess.create(level, beacon));
        helper.assertTrue(ModMenus.openBeacon(player).equals(Optional.of(beacon)), "The beacon of the menu");
        BeaconTestSupport.removeBeacon(helper);
        helper.succeed();
    }
}
