package io.github.nicolasgoyon.beaconhold.cost;

import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** The tags of the mod. */
public final class ModTags {
    /**
     * The items that a beacon accepts as consumable. A data pack can change the list. The default list: amethyst
     * shard, prismarine crystals and nether quartz.
     */
    public static final TagKey<Item> CONSUMABLES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(BeaconholdApi.MOD_ID, "beacon_consumables"));

    private ModTags() {
    }
}
