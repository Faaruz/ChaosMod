# Chaos Mod

Random client-side RuneLite events for livestreams. A visible countdown starts a randomly selected event; no chat bridge, external service, or voting setup is required.

## Configuration

`Default event duration` is used for every event by default. Each event also has an optional duration override: set it to `0` to inherit the default, or set a number of seconds for that event only. `Time until next event` controls the countdown shown after an event finishes. `Show event tile indicators` is off by default and only enables optional danger/target tiles.


## Included events

- Exploding Barrels
- Lights Out
- Spawn Jad
- Yama Meteor Shower
- Noob Outfit
- Giant Player
- Guitar Hero
- Vardorvis Captcha
- Dirty Screen
- CoX Portals
- Leviathan Boulders

## Adding another event

Create the event class in `com.chaosmod.events`, implement `ChaosMod`, then add one instance to `ChaosModFactory.createPool`. Events can independently use lifecycle, game-tick, client-tick, menu-click, and overlay-render hooks without adding event-specific code to `ChaosModPlugin`.

Effects are client-side only and never inject input, send game actions, or affect the game server. Some configured challenges temporarily consume matching local menu clicks as their stated penalty.

## Build and run

```text
./gradlew clean test
./gradlew run
```

Use RuneLite developer mode. Enable random events and set durations in the plugin configuration. The sidebar provides a separate button to test each event immediately.
