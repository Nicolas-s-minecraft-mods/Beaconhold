package io.github.nicolasgoyon.beaconhold.registry;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** The registries of the mod, and the data that the mod attaches to the beacons. */
public final class ModRegistries {
    /** The registry of the beacon functions. The clients get the ids of the functions from the server. */
    public static final Registry<BeaconFunction> FUNCTIONS =
            new RegistryBuilder<>(BeaconholdApi.FUNCTION_REGISTRY_KEY).sync(true).create();

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BeaconholdApi.MOD_ID);

    /** The id of the function of a beacon. The beacon saves it. A beacon with no function has no value. */
    public static final Supplier<AttachmentType<ResourceLocation>> BEACON_FUNCTION = ATTACHMENTS.register(
            "function",
            () -> AttachmentType.builder(() -> ResourceLocation.withDefaultNamespace("air"))
                    .serialize(ResourceLocation.CODEC)
                    .build());

    private ModRegistries() {
    }

    /**
     * Registers the registries and the attachments.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(NewRegistryEvent.class, event -> event.register(FUNCTIONS));
    }
}
