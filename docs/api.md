# Beaconhold API

The API lets another mod add functions to the beacon, and ask if a position is in the area of a beacon with a function. The API is the package `io.github.nicolasgoyon.beaconhold.api`. The other packages of the mod are not the API: they can change in each version.

## Add the dependency

Each release publishes the jar, the sources jar and the Javadoc jar to GitHub Packages. GitHub Packages needs a GitHub token with the `read:packages` scope, also for a public package.

```groovy
repositories {
    maven {
        name = 'Beaconhold'
        url = uri('https://maven.pkg.github.com/nicolas-s-minecraft-mods/beaconhold')
        credentials {
            username = providers.environmentVariable('GITHUB_ACTOR').getOrElse('')
            password = providers.environmentVariable('GITHUB_TOKEN').getOrElse('')
        }
    }
}

dependencies {
    // Replace 0.2.0 with the version of a release (tag v0.2.0).
    implementation 'io.github.nicolasgoyon.beaconhold:beaconhold:0.2.0'
}
```

Add Beaconhold to `neoforge.mods.toml`. Use `type = "optional"` if your mod also operates with no Beaconhold:

```toml
[[dependencies.examplemod]]
modId = "beaconhold"
type = "optional"
versionRange = "[0.2,)"
ordering = "NONE"
side = "BOTH"
```

With an optional dependency, call the API only when `ModList.get().isLoaded("beaconhold")` is true. Put the calls in a separate class, so that the game does not load the classes of the API when Beaconhold is not there.

## Register a function

A beacon has one function at a time. The function is an addition to the vanilla powers of the beacon.

```java
public final class ExampleFunctions {
    public static final DeferredRegister<BeaconFunction> FUNCTIONS =
            DeferredRegister.create(BeaconholdApi.FUNCTION_REGISTRY_KEY, "examplemod");

    public static final DeferredHolder<BeaconFunction, BeaconFunction> STORM_SHIELD = FUNCTIONS.register(
            "storm_shield", () -> BeaconFunction.builder().icon(() -> Items.LIGHTNING_ROD).build());
}

// In the constructor of your mod:
ExampleFunctions.FUNCTIONS.register(modEventBus);
```

The clients show the area of an active beacon as an energy dome. For a function that does not use the area of the beacon, call `showsArea(false)` on the builder. Then a beacon with this function shows no dome.

Add the name of the function to the language files of your mod. The key is `beacon_function.<namespace>.<path>`:

```json
{
  "beacon_function.examplemod.storm_shield": "Storm shield"
}
```

## Ask if a beacon covers a position

The queries operate on the server thread. They use the saved list of the beacons, so they do not load chunks.

```java
ResourceKey<BeaconFunction> shield = ExampleFunctions.STORM_SHIELD.getKey();

if (BeaconholdApi.isCovered(serverLevel, pos, shield)) {
    // The storm does not damage this block.
}

for (ActiveBeacon beacon : BeaconholdApi.beaconsCovering(serverLevel, pos, shield)) {
    int radius = beacon.area().radius();
}
```

- `isCovered(level, pos, function)`: true if an active beacon with the function covers the position.
- `beaconsCovering(level, pos, function)`: the active beacons with the function that cover the position.
- `activeBeacons(level)`: all the active beacons of a dimension.

The area is a vertical cylinder for the full height of the world. `BeaconArea.contains(pos)` uses only the X and Z coordinates. A beacon has one area for all its functions and for its vanilla powers.

The server options set the radii. A change of the options changes the areas at once. Each active beacon whose radius changes gives a `BeaconFunctionEvent.AreaChange` event (see [Events](#events)).

A function is active when all these conditions are true:

- The beacon has the function.
- The pyramid level is 1 or more.
- No block stops the beam.
- The beacon has fuel or a stock of the consumable.

A beacon with no fuel and no stock is off: it has no beam, no vanilla powers and no active function.

A beacon updates its state each 80 ticks (4 seconds). A beacon in a chunk that is not loaded keeps its last state. After a load of its chunk, an active beacon keeps its last state until it knows its pyramid level and its beam. Thus a restart of the server gives no `Stop` event and no `Start` event.

## Get and set the function of a beacon

```java
Optional<ResourceKey<BeaconFunction>> function = BeaconholdApi.getFunction(serverLevel, beaconPos);
boolean done = BeaconholdApi.setFunction(serverLevel, beaconPos, shield);
BeaconholdApi.setFunction(serverLevel, beaconPos, null); // removes the function
```

`setFunction` needs a beacon in a loaded chunk. It returns false if there is no beacon at the position.

## Events

The server posts the events on `NeoForge.EVENT_BUS`:

- `BeaconFunctionEvent.Start`: a beacon starts a function.
- `BeaconFunctionEvent.Stop`: a beacon stops a function. The causes:
  - A block stops the beam.
  - The pyramid is not complete.
  - The beacon has no consumable.
  - The function changes.
  - A player or an explosion removes the beacon.
- `BeaconFunctionEvent.AreaChange`: a change of the server options changes the area of an active function. `getOldArea()` gives the area before the change. `getBeacon().area()` gives the new area. The function stays active.

When each event occurs:

| Change | Events |
|---|---|
| A beacon gets a function, and it can operate | `Start` |
| A block stops the beam, the pyramid is not complete or the beacon has no consumable | `Stop` |
| The function of an active beacon changes | `Stop` with the old function, then `Start` with the new function |
| A player or an explosion removes an active beacon | `Stop` |
| The pyramid level of an active beacon changes | `Stop` with the old area, then `Start` with the new area |
| A change of the server options changes the radius of the pyramid level of an active beacon | `AreaChange` |
| A change of the server options does not change the radius (for example the limit of the chunks) | no event |
| A restart of the server, or a change of the options file of a stopped server | no event |

The server posts the `AreaChange` events after it applies the new options. Then the queries use the new areas.

```java
NeoForge.EVENT_BUS.addListener(BeaconFunctionEvent.Start.class, event -> {
    if (event.getBeacon().function().equals(shield)) {
        // Start the effect in event.getBeacon().area().
    }
});

NeoForge.EVENT_BUS.addListener(BeaconFunctionEvent.AreaChange.class, event -> {
    if (event.getBeacon().function().equals(shield)) {
        // Move the effect from event.getOldArea() to event.getBeacon().area().
    }
});
```
