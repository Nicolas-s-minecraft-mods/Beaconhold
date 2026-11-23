# Beaconhold design

Beaconhold is a mod for Minecraft 1.21.1 (NeoForge). It gives more functions to the beacon, and an API so that other mods add functions to the beacon.

## State

The mod has the API for other mods, the area of a beacon and the command for the operators. It has the function "no hostile spawns". This document gives the rules of the mod. Each rule that is not in the code yet has the mark **planned** and the number of its issue.

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
  4. The beacon has a stock of the consumable (see [Cost](#cost)). **Planned** (#8).
- A beacon with no active function has no effect of Beaconhold.

### No hostile spawns

- The id of the function is `beaconhold:no_hostile_spawns`.
- A hostile mob does not spawn in the area of an active beacon with this function.
- A hostile mob is a mob of the `monster` category of the game. Examples: zombie, skeleton, creeper, spider, phantom, slime.
- The function stops these spawns:
  - The natural spawns (the dark areas, and the phantoms).
  - The spawns at the generation of a chunk.
  - The spawns from a monster spawner, only when the server option `blockSpawners` is on. The default value is off.
- The function does not stop these spawns:
  - The patrols of pillagers.
  - The raids.
  - The trial spawners.
  - The spawns from a spawn egg, a command, a dispenser or a structure.
  - The reinforcements of zombies and the summons of a mob (for example the vexes of an evoker).
  - The conversions (for example a villager that becomes a witch) and the warden.
- The function does not remove the hostile mobs that are already in the area. A hostile mob can walk into the area.

### Loaded chunks

**Planned** (#5).

- An active beacon with this function keeps chunks near it loaded. The chunks tick as if a player is near them: the crops grow and the machines operate.
- The loaded area is smaller than the area of the other functions. The loaded area is a number of chunks, not a radius in blocks.
- The beacon loads its own chunk first, then the nearest chunks. The default numbers of chunks:

  | Pyramid level | Chunks | Shape |
  |---|---|---|
  | 1 | 1 | the chunk of the beacon |
  | 2 | 5 | a cross of 5 chunks |
  | 3 | 9 | a square of 3 × 3 chunks |
  | 4 | 13 | a circle with a radius of 2 chunks |

- The server option `maxLoadedChunks` sets the maximum number of chunks that all the beacons of the server load together. The default value is 256. When a beacon starts and the limit is near, the beacon loads fewer chunks. It always loads its nearest chunks first.
- The chunks stay loaded after a restart of the server.
- When the function stops, the beacon releases its chunks.

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
- The player selects the function in the beacon screen. The screen shows the functions and the cost of each function. A new selection replaces the current function. **Planned** (#7).
- The beacon saves its function. A beacon that a player breaks loses its function.

## Cost

**Planned** (#8).

- An active function uses a consumable. The player puts one stack of the consumable in the beacon.
- The consumable is an item of the game. The server option `consumables` gives the list of the items that the beacon accepts. The default list:
  - amethyst shard,
  - prismarine crystals,
  - nether quartz.
- The owner of the project did not confirm the items. The list stays a server option.
- One stack lasts about 10 game days (240,000 ticks). A stack of 64 items thus uses one item each 3,750 ticks (about 3 minutes).
- With no consumable, the function stops. The beacon keeps its vanilla powers.
- The beacon screen shows the stock. The player gets a sign when the stock is low.

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

**Planned** (#9).

- The area of an active function shows as a light energy dome.
- From far, the dome is a faint veil on the edge of the area.
- From near, the dome shows rings with particles. It is not a wall, and the player sees through it.
- The dome fades with the distance.
- A client option turns the dome off.
- The owner of the project selects the look from previews.

## Options

The server options are in `serverconfig/beaconhold-server.toml` in the world folder. The client options are in `config/beaconhold-client.toml`. The mod list of the game gives a screen to change the options.

| Option | Side | Default | Issue |
|---|---|---|---|
| `area.radiusLevel1` to `area.radiusLevel4` | server | 20, 47, 73, 100 | #6 |
| `noHostileSpawns.blockSpawners` | server | off | #4 |
| `loadedChunks.chunksLevel1` to `loadedChunks.chunksLevel4` | server | 1, 5, 9, 13 | #5 (planned) |
| `loadedChunks.maxLoadedChunks` | server | 256 | #5 (planned) |
| `consumables` | server | amethyst shard, prismarine crystals, nether quartz | #8 (planned) |
| `dome.enabled` | client | on | #9 (planned) |

## Open questions

The owner of the project gives the answers. Until then, the design uses the default values above, and an option keeps each value open.

1. The radius for each pyramid level: the default radii go from 20 to 100 blocks in equal steps. Are other values better?
2. Loaded chunks: are 13 chunks at level 4 correct for "about 15 chunks"? Is a limit for each player necessary, in addition to the limit of the server? A limit for each player needs the beacon to save the player that placed it.
3. No hostile spawns: do the raids, the trial spawners or the reinforcements of zombies also stop?
4. Consumable: which 2 or 3 items? Does each function use a different item?
5. Selection: until the beacon screen exists, the function needs a command. Is a command for operators correct?
