package io.github.nicolasgoyon.beaconhold.registry;

import com.mojang.serialization.Codec;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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

    /** The stock of the consumable of a beacon: one stack. The beacon saves it. */
    public static final Supplier<AttachmentType<ItemStack>> BEACON_STOCK = ATTACHMENTS.register(
            "stock",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.OPTIONAL_CODEC, stack -> !stack.isEmpty())
                    .build());

    /** The fuel of a beacon: the number of ticks that the last consumable item still gives. The beacon saves it. */
    public static final Supplier<AttachmentType<Integer>> BEACON_FUEL = ATTACHMENTS.register(
            "fuel",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT, fuel -> fuel != 0)
                    .build());

    /** The game time of the last update of the fuel of a beacon. The beacon saves it. */
    public static final Supplier<AttachmentType<Long>> BEACON_FUEL_TICK = ATTACHMENTS.register(
            "fuel_tick",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG, tick -> tick != 0L)
                    .build());

    /**
     * True when the beacon is off: it has no pyramid, no free beam, or no fuel and no stock. The beacon saves it, and
     * the clients get it, so that they hide the beam. A beacon is off until its first update.
     */
    public static final Supplier<AttachmentType<Boolean>> BEACON_OFF = ATTACHMENTS.register(
            "off",
            () -> AttachmentType.builder(() -> true)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    /**
     * The player that placed a beacon. The beacon saves it. A beacon that no player placed has no value. The default
     * value is not an owner: use {@code getExistingData}.
     */
    public static final Supplier<AttachmentType<UUID>> BEACON_OWNER = ATTACHMENTS.register(
            "owner",
            () -> AttachmentType.builder(() -> new UUID(0L, 0L))
                    .serialize(UUIDUtil.CODEC)
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
