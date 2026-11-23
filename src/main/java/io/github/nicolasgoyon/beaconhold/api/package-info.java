/**
 * The API of Beaconhold, for other mods.
 *
 * <ul>
 *   <li>{@link io.github.nicolasgoyon.beaconhold.api.BeaconFunction}: a function of a beacon, in the registry
 *       {@link io.github.nicolasgoyon.beaconhold.api.BeaconholdApi#FUNCTION_REGISTRY_KEY}.
 *   <li>{@link io.github.nicolasgoyon.beaconhold.api.BeaconholdApi}: the queries of the areas, and the function of a
 *       beacon.
 *   <li>{@link io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent}: the events for a beacon that starts or
 *       stops a function.
 * </ul>
 *
 * <p>Only the classes of this package are the API. The other packages of the mod can change in each version.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
package io.github.nicolasgoyon.beaconhold.api;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
