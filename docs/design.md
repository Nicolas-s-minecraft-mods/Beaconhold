# Beaconhold design

Beaconhold is a mod for Minecraft 1.21.1 (NeoForge). It gives more functions to the beacon, and an API so that other mods add functions to the beacon.

## State

The mod has the API for other mods, the area of a beacon and the command for the operators. It also has the function screen, the cost and the energy dome. It has the functions "no hostile spawns" and "loaded chunks". This document gives the rules of the mod. Each rule that is not in the code yet has the mark **planned** and the number of its issue.

## Terms

- **Pyramid level**: the number of complete layers of the pyramid under the beacon, from 0 to 4. The game calculates this number.
- **Beam**: the light beam of the beacon. A block that stops light (for example stone) stops the beam. Glass does not stop the beam.
- **Function**: a Beaconhold effect of a beacon, for example "no hostile spawns". A function is different from a vanilla power (Speed, Haste, Resistance, Jump Boost, Strength, Regeneration).
- **Area**: the space where the functions and the vanilla powers of a beacon have an effect. A beacon has one area.

## Functions

### Rules for all functions

- A beacon has one function at a time, or no function.
- The function of a beacon is an addition to the vanilla powers. The beacon keeps its vanilla powers. The vanilla powers use the area of the beacon (see [Area](#area)).
- A beacon is **on** when these conditions are true: the pyramid level is 1 or more, no block stops the beam, and the beacon has fuel or a stock of the consumable.
- Each other beacon is **off**: it has no beam, no vanilla powers, no function and no dome (see [Cost](#cost)).
- A beacon that is off is silent. It plays no ambient sound, no activation sound and no deactivation sound.
- A beacon plays the activation sound when it goes on, for example when a stock of the consumable turns it on. It plays the deactivation sound when it goes off: the pyramid is not complete, a block stops the beam, or the consumable ends.
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
  | 2 | 38 | 25 to 32 | 25 |
  | 3 | 57 | 52 to 56 | 54 |
  | 4 | 75 | 85 to 91 | 89 |

- The server option `maxLoadedChunks` sets the maximum number of chunks that all the beacons of the server load together. The default value is 512: enough for 5 beacons at the pyramid level 4.
- When a beacon starts and the limit is near, the beacon loads fewer chunks. It loads its own chunk first, then the nearest chunks.
- When a beacon releases its chunks, the beacons that got fewer chunks get the free capacity at the end of the tick. The beacons get their chunks in the order of the dimensions, then in the order of their start. A beacon that started first gets the free capacity first. No beacon gets fewer chunks because of a release.
- The command `/beaconhold chunks limit` shows the number of loaded chunks and the limit. The command `/beaconhold chunks limit <chunks>` sets the limit, applies it at once and saves the server options. Only an operator can use the command.
- The chunks stay loaded after a restart of the server. Each dimension saves the list of the chunks that each beacon loads. NeoForge saves the tickets of the chunks. When the server loads a world, it removes the tickets that the saved list of the chunks does not contain.
- When the server starts, it makes the tickets agree with the saved lists:
  - An active beacon with this function in the saved list of the beacons gets a ticket again for each of its saved chunks.
  - A saved position with no active beacon with this function releases its chunks.
  - Then the beacons get their chunks again with the current options, as after a change of the options. Thus a change of the options in the file of a stopped server applies at the start.
- When the function stops, the beacon releases its chunks. When a pyramid level changes, the beacon releases its chunks, then loads the chunks of the new area.
- A change of the server options applies at once (see [Options](#options)).

## Area

- A beacon has one area for all its functions and for its vanilla powers.
- The area is a vertical cylinder. Its axis goes through the center of the beacon.
- The area goes from the bottom to the top of the world. The height of the beacon has no effect on the area.
- A block is in the area when its horizontal distance to the beacon is equal to or less than the radius. The distance uses the X and Z coordinates of the two blocks.
- The pyramid level gives the radius. The default radii:

  | Pyramid level | Radius (blocks) |
  |---|---|
  | 1 | 20 |
  | 2 | 38 |
  | 3 | 57 |
  | 4 | 75 |

- Server options set the four radii.
- The vanilla powers of the beacon (Speed, Haste, Resistance, Jump Boost, Strength, Regeneration) apply to the players in the area. The area replaces the vanilla range (a box of 20 to 50 blocks around the beacon). A spectator gets no power.
- The server option `area.vanillaPowersUseArea` turns this rule off. Then the vanilla powers use the vanilla range.
- Each vanilla power of a beacon that is on has a bonus to its level. The server option `vanillaPowers.levelBonus` sets the bonus. The default value is 1: Speed I becomes Speed II, and Speed II becomes Speed III.
- The bonus is the exchange for the cost. With the server option `cost.consumableRequired` off, the vanilla powers get no bonus.
- A beacon that is off gives no vanilla power.
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
  - A beacon can have a pyramid and a free beam, but no fuel and no stock. Then the screen tells in red that the beacon is off because it has no consumable.
  - For the function "loaded chunks", the screen also shows the chunks that the beacon loads. It also shows the chunks that all the beacons of the server load, and the limit of the server.
  - The screen shows this text in red when the limit gives the beacon fewer chunks than its area. The tooltip of the text also gives the number of chunks of the area.
  - A click on a function selects it. A new selection replaces the current function. The server checks the selection.
  - The button "Back" opens the vanilla beacon screen again.
- The beacon saves its function. A beacon that a player breaks loses its function.

## Cost

- The consumable keeps the beacon on: its beam, its vanilla powers, its function and its dome. The cost does not depend on the function. A beacon with no function also uses the consumable.
- The consumable is an item of the game. The beacon accepts the items of the item tag `beaconhold:beacon_consumables`. The items are alternatives for the same fuel. The list:
  - amethyst shard,
  - prismarine crystals,
  - nether quartz.
- A data pack can change the tag.
- The player puts one stack of the consumable in the slot of the function screen. The slot accepts one stack of one item.
- A hopper, or a pipe of another mod, puts the consumable into the stock from all the sides of the beacon. It uses the item capability of the beacon. It puts items only into an empty stock or a stock of the same item, up to one stack. A hopper or a pipe cannot take items out of the stock.
- The server option `cost.ticksPerItem` gives the number of ticks for each item. The default value is 3,750 ticks, so one stack of 64 items lasts 10 game days.
- The beacon has a fuel: the ticks that the last item still gives. When the fuel is empty and the function can operate, the beacon takes one item from the stock. Each update removes the ticks of operation from the fuel.
- A beacon uses its consumable only when it has a pyramid and a free beam. A beacon in a chunk that is not loaded uses no consumable.
- With no fuel and no stock, the beacon is off. The beacon keeps its function. A new stock turns the beacon on again at its next update.
- A beacon that has no update yet is off. Thus a beacon in a world that has no stock is off until a player gives it the consumable.
- The clients get the power state of each beacon, and they hide the beam of a beacon that is off.
- The server option `cost.consumableRequired` turns the consumable off. Then the vanilla rule applies: a beacon with a pyramid and a free beam is on. The beacon keeps its stock and its fuel. The vanilla powers get no level bonus. A change of the option applies at the next update of each beacon.
- A comparator next to a beacon gives a signal from the level of the stock. The formula is the formula of a container with one slot. An empty stock gives 0, one item gives 1, and a full stack gives 15. The fuel of the last item does not count.
- The function screen shows the time that the stock and the fuel give, in game days. With less than one game day, the screen shows the text in red, and the beacon makes smoke at each update.
- An active beacon with less than one game day of stock and fuel warns the players in its area at each update. The client shows a red message above the hotbar and plays a bell sound. The client shows the warning of a beacon at most one time in 5 minutes.
- The client option `warnings.lowStock` turns the warning off. With the server option `cost.consumableRequired` off, the beacons send no warning.
- The beacon saves its stock, its fuel and the time of the last use of the fuel. They stay after a restart of the server.
- A beacon that a player or an explosion removes drops its stock. The fuel of the last item is lost.

## Saved data and updates

- Each dimension of the server saves a list of the beacons that have a function. For each beacon, the list keeps the position, the pyramid level, the function and the state (active or not).
- The beacon updates its entry each 80 ticks (4 seconds), at the same time as the vanilla powers.
- A beacon in a chunk that is not loaded keeps its last state. The queries use the saved list, so they do not load chunks.
- Each dimension keeps a spatial index of the areas of the active beacons, in memory. The index puts each area in the cells of a grid of 64 blocks. A query reads only the areas of the cell of the position. A change of a record updates the index at once. A change of the radii makes the index again at the next query.
- The game does not save the pyramid level and the beam. After a load of its chunk, a beacon scans its beam again, then calculates its pyramid level at the next update. Until then, an active beacon keeps its saved state and uses no consumable. A change of the function applies at once.
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
  - `AreaChange`: a change of the server options changes the area of an active function. The event gives the old area and the new area.
  - A change of the pyramid level stops the function with the old area, then starts it with the new area.
- **Publication**: the release publishes the jar, the sources jar and the Javadoc jar to GitHub Packages. Another mod compiles against this jar.

## Visual

- The area of a beacon shows as a light energy dome. The dome is on the edge of the area: a vertical circle around the beacon.
- A beacon shows the dome when one of these conditions is true:
  - The beacon has an active function, and the function shows its area.
  - The beacon is loaded and on (see [Rules for all functions](#rules-for-all-functions)).
- A beacon whose function does not show its area shows no dome. A beacon with no function shows no dome when the option `area.vanillaPowersUseArea` is off.
- From far, the dome is a faint veil. The veil goes 96 blocks above and below the eyes of the player, and it fades at its top and its bottom.
- Near the edge, the veil fades out. The dome shows a ring of light on the ground, with particles. It is not a wall, and the player sees through it.
- The veil fades with the distance. At 256 blocks from the edge, the client does not show the dome.
- In a cave, more than 6 blocks under the ground, the ring is at the level of the feet of the player.
- The default color of the dome is a light cyan (`#9EE8FF`). Client options set the color and the maximum opacity of the veil and of the ring. The default maximum opacities are 12 % for the veil and 50 % for the ring. An opacity of 0 hides the veil or the ring.
- Each function of the mod shows its area. A function of another mod can show no area (see [api.md](api.md)).
- The server sends to each player only the areas near the player. The edge of such an area is at 320 blocks or less from the player (horizontal distance). This distance is the distance of the dome (256 blocks) and a margin of 64 blocks for the movement of the player.
- Each second, and at the end of a tick in which an area starts or stops, the server calculates the areas near each player. It sends the list to a player only when the list changes.
- A player also gets the areas when the player joins the server, enters a dimension or respawns.
- The client option `dome.enabled` turns the dome off.

## Options

The server options are in `serverconfig/beaconhold-server.toml` in the world folder. The client options are in `config/beaconhold-client.toml`. The mod list of the game gives a screen to change the options.

A change of the server options applies at once to the active beacons. The server reads the file again after a change. The command `/beaconhold chunks limit <chunks>` also applies its change at once.

- The areas use the new radii. The queries of the API and the function "no hostile spawns" use the new areas.
- The clients get the areas again, so the domes show the new areas.
- A beacon with no function shows or hides its dome at once when the option `area.vanillaPowersUseArea` changes.
- The beacons with the function "loaded chunks" load the chunks of their new areas, with the new limit. The beacons get their chunks in the order of the dimensions, then in the order of their start. A beacon that started first keeps its chunks first.
- A change of the options gives no `Start` event and no `Stop` event. Each active beacon whose radius changes gives an `AreaChange` event, with the old area and the new area.

| Option | Side | Default | Issue |
|---|---|---|---|
| `area.radiusLevel1` to `area.radiusLevel4` | server | 20, 38, 57, 75 | #6, #44 |
| `area.vanillaPowersUseArea` | server | on | #44 |
| `vanillaPowers.levelBonus` | server | 1 | #47 |
| `noHostileSpawns.blockSpawners` | server | off | #4, #22 |
| `loadedChunks.maxLoadedChunks` | server | 512 | #5, #23 |
| `cost.ticksPerItem` | server | 3750 | #7 |
| `cost.consumableRequired` | server | on | #34 |
| `beaconhold:beacon_consumables` | item tag (data) | amethyst shard, prismarine crystals, nether quartz | #8 |
| `dome.enabled` | client | on | #9 |
| `dome.veilColor`, `dome.ringColor` | client | `#9EE8FF` | #33 |
| `dome.veilOpacity`, `dome.ringOpacity` | client | 12, 50 (percent) | #33 |
| `warnings.lowStock` | client | on | #30 |

## Open questions

The owner of the project gives the answers. Until then, the design uses the default values above, and an option keeps each value open.

1. Loaded chunks: is 512 a good default for the limit of the server? With the default radii, 512 chunks are enough for 5 beacons at the pyramid level 4. Is a limit for each player necessary, in addition to the limit of the server? A limit for each player needs the beacon to save the player that placed it.
2. No hostile spawns: do the reinforcements of zombies also stop?
3. Selection: the function screen opens from a button next to the vanilla beacon screen. Is a separate screen correct?
4. Dome: a function of another mod can hide its area (`showsArea(false)`). Does a beacon with such a function and an active beam show the dome? The code shows no dome.
5. Dome: with the option `area.vanillaPowersUseArea` off, does a beacon with no function show the dome? The code shows no dome.
6. Vanilla powers: does the level bonus also apply to Regeneration? The code applies it to all the powers.
7. Beam: does the beam also go off with no stock? The code hides it.
