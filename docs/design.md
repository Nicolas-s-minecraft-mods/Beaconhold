# Beaconhold design

Beaconhold is a mod for Minecraft 1.21.1 (NeoForge). It gives more functions to the beacon, and an API so that other mods add functions to the beacon.

## State

The mod loads in the game. It has no function yet. All the sections after this one are **planned work**: they come from the open issues, and the design issue (#3) sets the final rules.

## Planned functions

- **No hostile spawns** (#4): hostile mobs do not spawn near an active beacon. A beacon with no beam protects nothing. The design sets the area and the types of spawn that the beacon stops (natural spawns, spawners, raids, patrols).
- **Loaded chunks** (#5): a beacon keeps the chunks near it loaded. A beacon with more levels in its pyramid keeps more chunks loaded, up to about 15 chunks. A server option can limit the loaded chunks.

## Planned rules

- **Area**: the levels of the pyramid give the size of the area. The design sets the shape: a square of chunks or a radius in blocks. It also sets the height of the area.
- **Selection** (#7): the player selects one Beaconhold function in the beacon screen, with a cost. A beacon has one function at a time, as it has one vanilla power. The beacon saves its function.
- **Consumable** (#8): an active function uses a consumable from a stock in the beacon. One stack lasts about 10 game days. The item is not easy to farm. With no consumable, the function stops and the beacon keeps its vanilla power.
- **Energy dome** (#9): the area of an active function shows as a light dome. The player sees the dome from far, and sees through it from near. A client option turns the dome off.

## Planned API

The API is for other mods (#6). Example: Folkhold Events protects the blocks near a beacon from storms and earthquakes.

- A registry of beacon functions: id, name, icon and an optional cost.
- A query on the server: the beacons with a function that cover a position. The query does not load chunks. The saved data of the active beacons gives the answer.
- Events when a beacon starts or stops a function.
- The API is in its own package, with Javadoc. The release publishes the jar, so that other mods can compile against it.

## Open questions

The design issue (#3) asks the owner these questions:

1. Does a beacon have all its functions at the same time, or does the player select them in the beacon screen?
2. Does a function have a cost (an item, a level of the pyramid)?
3. Is the area a square of chunks or a radius in blocks? How large is it for each level of the pyramid?
4. Does the area go from the bottom to the top of the world?
