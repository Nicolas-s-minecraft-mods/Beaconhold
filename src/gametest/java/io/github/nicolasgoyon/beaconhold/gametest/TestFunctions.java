package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

/** A beacon function for the GameTests, and a record of the events of the beacon functions. */
@EventBusSubscriber(modid = Beaconhold.MOD_ID)
public final class TestFunctions {
    /** A function with no effect. Only the GameTests use it. */
    public static final ResourceKey<BeaconFunction> TEST = ResourceKey.create(
            BeaconholdApi.FUNCTION_REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(Beaconhold.MOD_ID, "test"));

    /** A function with no effect and no energy dome. Only the GameTests use it. */
    public static final ResourceKey<BeaconFunction> NO_AREA = ResourceKey.create(
            BeaconholdApi.FUNCTION_REGISTRY_KEY,
            ResourceLocation.fromNamespaceAndPath(Beaconhold.MOD_ID, "test_no_area"));

    private static final List<BeaconFunctionEvent> EVENTS = new ArrayList<>();

    static {
        NeoForge.EVENT_BUS.addListener(BeaconFunctionEvent.Start.class, TestFunctions::record);
        NeoForge.EVENT_BUS.addListener(BeaconFunctionEvent.Stop.class, TestFunctions::record);
    }

    private TestFunctions() {
    }

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(BeaconholdApi.FUNCTION_REGISTRY_KEY, helper -> {
            helper.register(TEST, BeaconFunction.builder().icon(() -> Items.STONE).build());
            helper.register(
                    NO_AREA,
                    BeaconFunction.builder().icon(() -> Items.DIRT).showsArea(false).build());
        });
    }

    private static synchronized void record(BeaconFunctionEvent event) {
        EVENTS.add(event);
    }

    /**
     * Gives the events of a beacon, the oldest first.
     *
     * @param pos the absolute position of the beacon
     * @return a copy of the events
     */
    public static synchronized List<BeaconFunctionEvent> eventsAt(BlockPos pos) {
        return EVENTS.stream().filter(event -> event.getBeacon().pos().equals(pos)).toList();
    }
}
