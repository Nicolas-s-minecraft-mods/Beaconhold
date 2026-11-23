package io.github.nicolasgoyon.beaconhold.api;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * A function of a beacon, for example "no hostile spawns".
 *
 * <p>A beacon has one function at a time, or no function. The function is an addition to the vanilla powers of the
 * beacon. Register each function in the registry {@link BeaconholdApi#FUNCTION_REGISTRY_KEY}, for example with a
 * {@code DeferredRegister}:
 *
 * <pre>{@code
 * DeferredRegister<BeaconFunction> FUNCTIONS =
 *         DeferredRegister.create(BeaconholdApi.FUNCTION_REGISTRY_KEY, "examplemod");
 * DeferredHolder<BeaconFunction, BeaconFunction> STORM_SHIELD =
 *         FUNCTIONS.register("storm_shield", () -> BeaconFunction.builder().icon(() -> Items.LIGHTNING_ROD).build());
 * }</pre>
 *
 * <p>The name of the function is the translation key {@code beacon_function.<namespace>.<path>} of its id. Add this
 * key to the language files of your mod.
 */
public final class BeaconFunction {
    private final Supplier<? extends ItemLike> icon;

    private BeaconFunction(Builder builder) {
        this.icon = builder.icon;
    }

    /**
     * Gives a new builder of a function.
     *
     * @return a builder with the default values
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gives the icon of the function.
     *
     * @return a new stack of one item
     */
    public ItemStack icon() {
        return new ItemStack(icon.get());
    }

    /**
     * Gives the translation key of the name of a function.
     *
     * @param id the id of the function
     * @return the key {@code beacon_function.<namespace>.<path>}
     */
    public static String descriptionId(ResourceLocation id) {
        return Util.makeDescriptionId("beacon_function", id);
    }

    /**
     * Gives the name of a function, as a translatable text.
     *
     * @param function the key of the function
     * @return the name of the function
     */
    public static Component name(ResourceKey<BeaconFunction> function) {
        return Component.translatable(descriptionId(function.location()));
    }

    /** A builder of a {@link BeaconFunction}. */
    public static final class Builder {
        private Supplier<? extends ItemLike> icon = () -> Items.BEACON;

        private Builder() {
        }

        /**
         * Sets the icon of the function. The default icon is the beacon.
         *
         * @param icon the item of the icon. The builder reads the item only when the game shows the icon.
         * @return this builder
         */
        public Builder icon(Supplier<? extends ItemLike> icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        /**
         * Makes the function.
         *
         * @return a new function
         */
        public BeaconFunction build() {
            return new BeaconFunction(this);
        }
    }
}
