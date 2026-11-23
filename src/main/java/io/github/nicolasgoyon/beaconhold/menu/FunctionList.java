package io.github.nicolasgoyon.beaconhold.menu;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;

/**
 * The sorted list of the functions of the registry. The server and the client have the same registry, so they have the
 * same list.
 */
public final class FunctionList {
    private FunctionList() {
    }

    /**
     * Gives the functions of the registry, sorted by id.
     *
     * @return a new list
     */
    public static List<ResourceKey<BeaconFunction>> sorted() {
        return ModRegistries.FUNCTIONS.registryKeySet().stream()
                .sorted(Comparator.comparing(ResourceKey::location))
                .toList();
    }

    /**
     * Gives the index of a function in the sorted list.
     *
     * @param function the function, or an empty value
     * @return the index, or -1 for no function or for a function that is not in the list
     */
    public static int indexOf(Optional<ResourceKey<BeaconFunction>> function) {
        return function.map(key -> sorted().indexOf(key)).orElse(-1);
    }
}
