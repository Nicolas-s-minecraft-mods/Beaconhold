# Beaconhold design

Beaconhold is a mod for Minecraft 1.21.1 (NeoForge). It gives more functions to the beacon, and an API so that other mods add functions to the beacon.

## State

The mod has the API for other mods, the area of a beacon and the command for the operators. It also has the function screen, the cost and the energy dome. It has the functions "no hostile spawns" and "loaded chunks". This document gives the rules of the mod. Each rule that is not in the code yet has the mark **planned** and the number of its issue.

## Terms

- **Pyramid level**: the number of complete layers of the pyramid under the beacon, from 0 to 4. The game calculates this number.
- **Beam**: the light beam of the beacon. A block that stops light (for example stone) stops the beam. Glass does not stop the beam.
- **Function**: a Beaconhold effect of a beacon, for example "no hostile spawns". A function is different from a vanilla power (Speed, Haste, Resistance, Jump Boost, Strength, Regeneration).
- **Area**: the space where a function has an effect.

## Functions

### Rules for all functions

- A beacon has one function at a time, or no function.
- The function of a beacon is an addition to the vanilla powers. The beacon keeps its vanilla powers, and the function does not change them.
- A function is **active** when all these conditions are true:
  1. The beacon has a function.
  2. The pyramid level is 1 or more.
  3. No block stops the beam.
  4. The beacon has fuel or a stock of the consumable (see [Cost](#cost)).
- A beacon with no active function has no effect of Beaconhold.

### No hostile spawns

- The id of the function is `beaconhold:no_hostile_spawns`.
- A hostile mob does not spawn in the area of an active beacon with this function.
- A hostile mob is a mob of the `monster` category of the game. Examples: zombie, skeleton, creeper, spider, phantom, slime.
- The function stops these spawns:
  - The natural spawns (the dark areas, and the phantoms).
  - The spawns at the generation of a chunk.
  - The spawns from a monster spawner or a trial spawner, only when the server option `blockSpawners` is on. The default value is off.
- The function does not stop these spawns:
  - The patrols of pillagers.
  - The raids.
  - The spawns that a player starts: a spawn egg, a command, a dispenser or a bucket.
  - The spawns of a structure.
  - The reinforcements of zombies and the summons of a mob (for example the vexes of an evoker).
  - The conversions (for example a villager that becomes a witch) and the warden.
- The function does not remove the hostile mobs that are already in the area. A hostile mob can walk into the area.

### Loaded chunks

- The id of the function is `beaconhold:loaded_chunks`.
- An active beacon with this function keeps the chunks of its area loaded. The chunks tick as if a player is near them: the crops grow and the machines operate.
- The area is the same column as for the other functions (see [Area](#area)). The beacon loads each chunk that the area touches: a chunk with at least one column of blocks in the area.
- The number of chunks depends on the position of the beacon in its chunk. The numbers for the default radii:

  | Pyramid level | Radius (blocks) | Chunks | Chunks for a beacon in the center of its chunk |
  |---|---|---|---|
  | 1 | 20 | 9 to 13 | 9 |
  | 2 | 47 | 36 to 41 | 39 |
  | 3 | 73 | 81 to 88 | 85 |
  | 4 | 100 | 144 to 151 | 145 |

- The server option `maxLoadedChunks` sets the maximum number of chunks that all the beacons of the server load together. The default value is 512: enough for 3 beacons at the pyramid level 4.
- When a beacon starts and the limit is near, the beacon loads fewer chunks. It loads its own chunk first, then the nearest chunks. When another beacon releases its chunks, the beacon does not get more chunks. It gets them when it starts its function again.
- The command `/beaconhold chunks limit` shows the number of loaded chunks and the limit. The command `/beaconhold chunks limit <chunks>` sets the limit and saves the server options. Only an operator can use the command.
- The chunks stay loaded after a restart of the server. NeoForge saves the tickets of the chunks. When the server loads a world, it removes the tickets that the saved list of the beacons does not contain.
- When the function stops, the beacon releases its chunks. When a pyramid level changes, the beacon releases its chunks, then loads the chunks of the new area.
- A change of the options has an effect when a beacon starts its function again.

## Area

- The area is a vertical cylinder. Its axis goes through the center of the beacon.
- The area goes from the bottom to the top of the world. The height of the beacon has no effect on the area.
- A block is in the area when its horizontal distance to the beacon is equal to or less than the radius. The distance uses the X and Z coordinates of the two blocks.
- The pyramid level gives the radius. The default radii:

  | Pyramid level | Radius (blocks) |
  |---|---|
  | 1 | 20 |
  | 2 | 47 |
  | 3 | 73 |
  | 4 | 100 |

- Server options set the four radii.
- The area of a function of another mod uses the same rules. The other mod can use its own area in its own code.

## Selection of the function

- An operator sets the function of a beacon with a command:
  - `/beaconhold function set <position> <function>` sets the function.
  - `/beaconhold function clear <position>` removes the function.
  - `/beaconhold function get <position>` shows the function and its state.
- The player selects the function in the function screen of the beacon:
  - The button "Functions" next to the vanilla beacon screen opens the function screen. The rules of the vanilla beacon screen (distance, lock) also apply to the function screen.
  - The function screen lists the functions of the registry, sorted by id, with an icon. The entry "No function" removes the function. With more than 5 entries, the screen has pages.
  - The screen shows the function of the beacon, its state (active or not active), the stock of the consumable and the cost.
  - A click on a function selects it. A new selection replaces the current function. The server checks the selection.
  - The button "Back" opens the vanilla beacon screen again.
- The beacon saves its function. A beacon that a player breaks loses its function.

## Cost

- The consumable keeps the function of the beacon active. The cost does not depend on the function, and all the functions use the same consumable.
- The consumable is an item of the game. The beacon accepts the items of the item tag `beaconhold:beacon_consumables`. The items are alternatives for the same fuel. The list:
  - amethyst shard,
  - prismarine crystals,
  - nether quartz.
- A data pack can change the tag.
- The player puts one stack of the consumable in the slot of the function screen. The slot accepts one stack of one item.
- The server option `cost.ticksPerItem` gives the number of ticks for each item. The default value is 3,750 ticks, so one stack of 64 items lasts 10 game days.
- The beacon has a fuel: the ticks that the last item still gives. When the fuel is empty and the function can operate, the beacon takes one item from the stock. Each update removes the ticks of operation from the fuel.
- A beacon uses its consumable only when it has a function, a pyramid and a free beam. A beacon in a chunk that is not loaded uses no consumable.
- With no fuel and no stock, the function stops. The beacon keeps its function and its vanilla powers. A new stock starts the function again.
- The function screen shows the time that the stock and the fuel give, in game days. With less than one game day, the screen shows the text in red, and the beacon makes smoke at each update.
- A beacon that a player or an explosion removes drops its stock. The fuel of the last item is lost.

## Saved data and updates

- Each dimension of the server saves a list of the beacons that have a function. For each beacon, the list keeps the position, the pyramid level, the function and the state (active or not).
- The beacon updates its entry each 80 ticks (4 seconds), at the same time as the vanilla powers.
- A beacon in a chunk that is not loaded keeps its last state. The queries use the saved list, so they do not load chunks.
- When a loaded chunk has no beacon at the position of an entry, the server removes the entry.

## API

The API is for other mods. [api.md](api.md) gives the dependency and examples. For example, Folkhold Events can protect the blocks near a beacon from storms and earthquakes.

- The API is in the package `io.github.nicolasgoyon.beaconhold.api`. Each public class has Javadoc.
- **Registry**: a NeoForge registry of beacon functions (`beaconhold:beacon_function`). Another mod registers its functions with a `DeferredRegister`. A function has an id, a name (a translation key from the id) and an icon.
- **Queries** (on the server thread):
  - the active beacons that cover a position with a given function,
  - "is a position in the area of an active beacon with a given function",
  - all the active beacons of a dimension.
- **Selection**: get and set the function of a beacon.
- **Events** on the NeoForge event bus:
  - `Start`: a beacon starts a function.
  - `Stop`: a beacon stops a function.
  - A change of the pyramid level stops the function with the old area, then starts it with the new area.
- **Publication**: the release publishes the jar, the sources jar and the Javadoc jar to GitHub Packages. Another mod compiles against this jar.

## Visual

- The area of an active function shows as a light energy dome. The dome is on the edge of the area: a vertical circle around the beacon.
- From far, the dome is a faint veil. The veil goes 96 blocks above and below the eyes of the player, and it fades at its top and its bottom.
- Near the edge, the veil fades out. The dome shows a ring of light on the ground, with particles. It is not a wall, and the player sees through it.
- The veil fades with the distance. At 256 blocks from the edge, the client does not show the dome.
- In a cave, more than 6 blocks under the ground, the ring is at the level of the feet of the player.
- The color of the dome is a light cyan.
- Each function of the mod has a dome. A function of another mod can have no dome (see [api.md](api.md)).
- The server sends the areas of the active beacons of a dimension to the players in this dimension. It sends them when a function starts or stops, and when a player enters the dimension.
- The client option `dome.enabled` turns the dome off.

## Options

The server options are in `serverconfig/beaconhold-server.toml` in the world folder. The client options are in `config/beaconhold-client.toml`. The mod list of the game gives a screen to change the options.

| Option | Side | Default | Issue |
|---|---|---|---|
| `area.radiusLevel1` to `area.radiusLevel4` | server | 20, 47, 73, 100 | #6 |
| `noHostileSpawns.blockSpawners` | server | off | #4, #22 |
| `loadedChunks.maxLoadedChunks` | server | 512 | #5, #23 |
| `cost.ticksPerItem` | server | 3750 | #7 |
| `beaconhold:beacon_consumables` | item tag (data) | amethyst shard, prismarine crystals, nether quartz | #8 |
| `dome.enabled` | client | on | #9 |

## Open questions

The owner of the project gives the answers. Until then, the design uses the default values above, and an option keeps each value open.

1. Loaded chunks: is 512 a good default for the limit of the server? Is a limit for each player necessary, in addition to the limit of the server? A limit for each player needs the beacon to save the player that placed it.
2. No hostile spawns: do the reinforcements of zombies also stop?
3. Consumable: does a beacon with no function also use the consumable?
4. Selection: the function screen opens from a button next to the vanilla beacon screen. Is a separate screen correct?
