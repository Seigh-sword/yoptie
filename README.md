# Yoptie

Yoptie is a Fabric client-side optimization and debloat layer for Minecraft 26.2.

It does not add features. It deletes cost. Yoptie strips the parts of the vanilla client
that quietly burn frames, memory and network for nothing you can see, then puts hard
bounds on the parts that grow without limit: particles, entity render work, block entity
render work and render distance.

The target is a game that looks like the game at full quality while sitting under a 2 GB
heap.

## What it does

| Area | Vanilla behaviour | Yoptie behaviour | Default |
| --- | --- | --- | --- |
| Telemetry | Client reports game and hardware statistics to Mojang, including an optional extra data channel | Reporting is refused at the source and the telemetry log directory is never opened, so no event is built, no file is written and nothing is uploaded | Removed |
| Particles | Every render group can hold 16384 particles, and nothing bounds the total. Heavy scenes produce thousands of live objects per second | A global budget caps how many particles the engine will accept. Past the budget, new particles are dropped before they are queued | 4000 particles |
| Entities | Every entity in the view frustum inside 64 blocks of the camera gets a fresh render state built every frame, even when hidden by distance | Entities past the configured distance never reach the renderer. The camera entity, anything carrying the player and whatever the crosshair is on are always kept | 48 blocks, 64 for players |
| Block entities | Every visible block entity within render distance is extracted and submitted every frame | Extraction stops beyond the configured distance. Beacons, conduits, end portals and end gateways are never culled because they matter at range | 64 blocks |
| Render distance | Up to 32 chunks, which multiplies chunk memory, mesh memory and entity work | The effective render distance is clamped to a ceiling. Set the slider higher and Yoptie holds it here | 16 chunks |
| Entity ticks | Disabled | Optional: entities well past the render radius stop being ticked on the client. Off by default because it can snap animations when something re-enters view | Off |

Everything above is switchable in `config/yoptie.json`, which is written next to your
other Fabric configs on first launch.

## Configuration

`config/yoptie.json`, written with defaults on first launch:

```json
{
  "enabled": true,
  "telemetry": {
    "disabled": true
  },
  "particles": {
    "limitTotal": true,
    "maxParticles": 4000
  },
  "entities": {
    "distanceCulling": true,
    "entityDistance": 48.0,
    "playerDistance": 64.0,
    "blockEntityCulling": true,
    "blockEntityDistance": 64.0,
    "tickCulling": false,
    "tickCullPlayers": false
  },
  "renderDistance": {
    "capEnabled": true,
    "maxChunks": 16
  }
}
```

Set `enabled` to `false` to keep the mod installed but inert. Every other value is read
on startup and clamped to a sane range, so a typo cannot break the client.

## Verified against the real client

Yoptie ships a test that loads Minecraft 26.2 itself through the Fabric loader and checks
that every mixin in `yoptie.client.mixins.json` is applied to the real target class, that
every hook lands on a method that exists with the expected arguments, that every shadowed
field exists with the expected type, and that the config defaults and clamps behave. CI
fails if any of that stops being true, so a renamed or moved method in a new Minecraft
build cannot silently disable a hook.

## Requirements

- Minecraft 26.2, Java 25
- Fabric Loader 0.19.5 or newer
- No Fabric API install required, Yoptie only depends on the loader

## Installing

Drop the built jar into `.minecraft/mods` and launch Fabric 26.2.

## Building

```bash
./gradlew build
```

The jar lands in `build/libs/`. Gradle needs a JDK 25 toolchain and network access the
first time, because Loom downloads Minecraft and its libraries.

## Getting under 2 GB

Yoptie removes allocation churn, but the heap ceiling is still set by your launcher:

```bash
-Xmx2G -XX:+UseZGC -XX:+ZGenerational
```

Minecraft 26.1 and newer already default to Generational ZGC, so in most launchers you
only need `-Xmx2G`. If the game still fights the limit, lower `maxChunks` in the config
before anything else, it is by far the biggest memory lever.

## Compatibility notes

- Every hook is a client-side mixin. Yoptie never touches server logic and works on any
  server, including vanilla.
- Other rendering mods work alongside it. Yoptie only subtracts work that is already
  invisible: it does not replace the renderer, so it stacks with Sodium and friends.
- Because culling is distance based, crowded scenes will hide far away entities and
  block entities sooner than vanilla. Raise `entityDistance` and `blockEntityDistance`
  if you want vanilla range back.

## License

MIT. See `LICENSE`.
