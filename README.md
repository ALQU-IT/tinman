# Tin Man

A Fabric mod for **Minecraft Java Edition 26.2** that adds Voltite ore, a data-driven crafting
station called the Assembler, and a powered high-tech armour set with matching energy weapons.

---

## What you need

| Thing | Version | Notes |
|---|---|---|
| Minecraft | 26.2 | |
| Java | **25 or newer** | 26.2 requires it; the build targets release 25 |
| Fabric Loader | 0.19.3+ | |
| Fabric API | 0.158.0+26.2 | required at runtime, downloaded by Gradle for dev |
| Gradle | 9.5.1 | supplied by the wrapper — don't install it yourself |

Mappings are Mojang's official mappings, which is what Fabric Loom uses for 26.x by default.
There is no Yarn mapping for this version, so no `yarn_mappings` entry exists in
`gradle.properties`.

## Building

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```

The mod jar lands in `build/libs/tinman-1.0.0.jar` — that is the one to install.
Ignore `tinman-1.0.0-sources.jar`.

## Running it

**In a normal game:** install Fabric Loader 0.19.3 for 26.2, drop
`fabric-api-0.158.0+26.2.jar` and `tinman-1.0.0.jar` into `.minecraft/mods/`, and launch.

**In a dev environment:**

```bash
./gradlew runClient     # client with the mod loaded
./gradlew runServer     # dedicated server; accept the EULA in run/eula.txt first
```

On a dedicated server the mod must be installed on **both** sides: the client needs it for the
HUD, screens and particles, and the server needs it for everything else.

## Configuration

Written to `config/tinman.json` on first launch and re-saved on load, so new options appear
automatically after an update. Server-side values are authoritative.

> **Upgrading from an earlier build:** values already in your `config/tinman.json` are kept, by
> design — only genuinely new keys are added. That means a rebalance of existing keys does **not**
> reach a world you have already run. To pick up new defaults, delete `config/tinman.json` and let
> it regenerate, or edit the individual numbers by hand. The file carries a `configVersion`,
> and the mod logs a warning at startup when yours is behind the build, so a rebalance never
> silently looks like it did nothing.

```jsonc
{
  "worldgen": {
    "voltiteOreEnabled": true,
    "voltiteVeinsPerChunk": 3.5   // fractional values work: 3.5 = three veins plus a coin flip
  },
  "suit": {
    "batteryCapacity": 250000,
    "conservationPerLevel": 0.15,   // energy discount per Conservation level
    "unibeamEnabled": true,
    "unibeamDamage": 50.0,
    "unibeamRange": 32.0,
    "unibeamEnergyCost": 400,
    "unibeamCooldownTicks": 40,
    "flightLeanEnabled": true,
    "flightLeanFullSpeed": 0.35,    // horizontal blocks/tick for a full lean
    "flightLeanRate": 0.05,         // lean gained or shed per tick; smaller is slower           // per armour piece
    "flightEnabled": true,
    "flightDrainPerSecond": 20,
    "boostDrainMultiplier": 3.0,
    "boostSpeed": 0.085,
    "chargingStationRate": 100,   // energy per second
    "energyPerIngot": 2500        // what one Voltite Ingot is worth
  },
  "weapons": {
    "pulseDamage": 30.0,
    "chargedPulseDamage": 90.0,
    "pulseEnergyCost": 25,
    "chargedPulseEnergyCost": 150,
    "pulseCooldownTicks": 2,          // 10 shots a second
    "pulseChargeTicks": 10,
    "pulseVelocity": 3.2,
    "chargedPulseVelocity": 3.8,
    "chargedShotEnabled": true,
    "chargedShotExplosionRadius": 7.0,
    "chargedShotBreaksBlocks": true,
    "blockLaunchChance": 0.45,        // share of broken blocks thrown as debris
    "blockLaunchPower": 0.55,
    "maxLaunchedBlocks": 140,          // per blast, so a big radius cannot flood the server
    "bladeEnergyBonusDamage": 15.0,
    "bladeEnergyCostPerHit": 15
  }
}
```

---

## Contents

### Voltite

Voltite Ore and Deepslate Voltite Ore generate between **Y -40 and Y 16** in two vein sizes (4 and
6). They need an **iron pickaxe or better**, drop 1–2 Raw Voltite (affected by Fortune) or the ore
block itself with Silk Touch, and give 3–7 XP.

Measured over 401 generated chunks with the default rate: about **14 Voltite per chunk against 23
diamond**, so it is meaningfully rarer than diamond overall while being concentrated in a much
narrower band. Turn `voltiteVeinsPerChunk` up or down to taste.

Smelt or blast Raw Voltite (or the ore) into a **Voltite Ingot**. Nine nuggets make an ingot, nine
ingots make a **Block of Voltite** (which glows faintly at light level 4).

**Tools** — Voltite Pickaxe, Axe, Shovel and Hoe are crafted in a **normal crafting table**. They
are deliberately overpowered: netherite mining tier, speed 32 (netherite is 9), 6000 durability
and +10 attack damage.

### The Assembler

Crafted in a normal crafting table:

```
I V I     I = iron ingot      V = Voltite Ingot
I C I     C = crafting table  B = iron block
B V B
```

It has a 3×3 grid, a **power cell** slot that only takes Voltite Ingots, and an output slot. An
assembly takes 3 seconds and consumes ingots from the power cell.

It uses its **own recipe type**, `tinman:assembling`, loaded from JSON, so server admins and
datapacks can add their own. The syntax matches `minecraft:crafting_shaped`, plus an optional
`power_cost` (default 1):

```json
{
  "type": "tinman:assembling",
  "key": { "V": "tinman:voltite_ingot", "I": "minecraft:iron_block" },
  "pattern": ["VVV", "V V", "IRI"],
  "result": { "id": "tinman:tin_man_helmet" },
  "power_cost": 2
}
```

It has a **recipe book**, the same widget the crafting table uses, listing only
`tinman:assembling` recipes under one tab. Clicking a recipe lays it out in the grid as a ghost,
and the usual click-to-fill and shift-click-to-fill-max both work. Recipes show up once unlocked,
which happens as soon as you own an Assembler.

The Assembler drops its contents when broken, emits a comparator signal, and **works with
hoppers** — fed from above, powered from the sides, results pulled from below. Items inserted by a
hopper are spread across the grid rather than piled into one slot, so multi-slot recipes can be
automated.

Leaving a **single** piece of energy gear in the grid with ingots in the power cell recharges it
instead of crafting; once full it is ejected to the output slot.

### The Tin Man suit

All four pieces are **Assembler-only**, enchantable, armour-trim compatible, and repaired with
Voltite Ingots.

The material is **exactly double netherite** on every defensive axis: 6/12/16/6 defence against
netherite's 3/6/8/3, 6.0 toughness against 3.0, 0.2 knockback resistance against 0.1, on double
the durability multiplier.

Vanilla then clamps the armour attribute at 30 and toughness at 20, so a worn set reads **30 / 20 /
0.8** in game against netherite's **20 / 12 / 0.4**. The clamp costs less than it looks, because
the damage formula is
`clamp(armour - damage / (2 + toughness / 4), armour * 0.2, 20) / 25`: the extra toughness is what
keeps a big hit from dragging the armour term down. Netherite starts falling off the 80% reduction
ceiling immediately; this holds it up to a 70-damage hit.

| Incoming hit | Netherite takes | Tin Man takes |
|---|---|---|
| 10 | 2.8 | **2.0** |
| 20 | 7.2 | **4.0** |
| 40 | 20.8 | **8.0** |
| 70 | 53.2 | **14.0** |
| 100 | 84.0 | **37.1** |

The suit stores no energy itself. Its abilities run off a **Voltite Battery** carried anywhere in
your inventory — so does everything else powered in the mod.

**Full set with charge remaining:**

- Creative-style flight, draining energy per second. Hold sprint while flying to boost — faster,
  costlier, with thruster flames from the boots.
- Immunity to fall and fire damage.
- The suit **leans into the dive** as you pick up forward speed and eases back upright as you
  slow, exactly the elytra pose. It eases both ways rather than snapping, and other players see
  it too.
- A chest-mounted **unibeam**, fired with a key (**R** by default, rebindable in Controls). It
  lances out from the chest, stops at the first solid block, and damages *everything* it passes
  through rather than only the first target. Damage, range, energy cost, cooldown and the ability
  itself are all config options.
- Helmet HUD: energy bar (red below 15%), altitude, and the name and health of whatever your
  crosshair is on.
- Night vision underwater and in the dark.
- **Energy hits zero and flight cuts out that same tick**, with a power-down sound. The armour
  keeps working as ordinary protection.

Individual pieces worn alone give protection only.

**Recharging** — batteries are the only thing that holds a charge, so they are the only thing you
recharge. Either put one in the Assembler with ingots in the power cell, or use the
**Charging Station**, which burns ingots into a buffer and trickles it into its own four gear slots
and any suit worn within four blocks.

The Station charges **everything at once** rather than one item at a time: the per-tick energy
budget is split evenly across every piece that still has room, so dropping a whole suit into its
four slots fills all four in lockstep. Anything that fills up drops out and its share is re-split
among the rest. Gear in the slots is served first; whatever budget is left over goes to suits worn
nearby.

### The Voltite Battery

One item holds all the energy in the mod. Craft it in a normal crafting table (iron, Voltite
Ingots and a redstone block), charge it, and carry it; the suit and both weapons draw from any
battery in your inventory. It holds 100,000 by default, stored in a `tinman:energy` data
component so charge survives death, chests and multiplayer.

**Conservation** (I–III) is a custom enchantment for batteries, obtainable at an enchanting table.
Each level takes 15% off the energy every action costs, so a 100-energy action costs 85, 70 or 55.
When several batteries are carried, the highest Conservation level among them applies — taking the
best rather than whichever battery happens to drain first keeps a given action costing the same
regardless of how your inventory is ordered.

### Weapons

Both are **Assembler-only** and draw on their own charge first, falling back to the worn suit, so
firing never drains your flight reserve first.

- **Pulse Gauntlet** — tap right-click to fire an energy bolt (damage plus knockback, short
  cooldown). Hold to charge a heavier shot that also sets off a small blast. The blast damages mobs
  and **tears up terrain**: it carves a ragged crater and throws a share of the debris outward as
  real falling blocks rather than quiet drops. Bedrock and anything else unbreakable is left
  alone. The whole charged shot can be switched off in the config, and terrain damage can be
  turned off on its own with `chargedShotBreaksBlocks`.

  It is a 3D model rather than a flat sprite, shaped as a sleeve that sits around the forearm, and
  its held-hand display transforms slide it back off the fist so it reads as worn rather than
  gripped. If the placement looks off in game, the numbers to nudge are
  `display.thirdperson_righthand.translation` in
  `assets/tinman/models/item/pulse_gauntlet.json`: the second value slides it along the arm, the
  third moves it toward or away from the elbow.
- **Voltite Blade** — a netherite sword lands 8 damage; this lands **31** (27.0 material bonus +
  the 3.0 sword baseline + the player's 1), on 6000 durability with a faster swing. Deals a
  further +15 while you have charge to spend, with electric sparks on hit.

### Advancements

Mine your first Voltite → build an Assembler → craft the full suit → fly 1000 blocks. Flight
distance is tracked persistently, so progress survives logging out.

---

## How it is put together

`src/main` is common code, `src/client` is client-only — Loom's split source sets mean
client-only classes physically cannot be referenced from server code by accident.

Energy consumption, flight permission, recipe matching and projectile logic are all
**server-side**. The client does no gameplay decision-making: it reads the energy component
(which vanilla already syncs along with equipped stacks) to draw the HUD, and receives flight
permission through the vanilla abilities packet. Particles the player shouldn't be alone in seeing
are broadcast from the server with `sendParticles`, so nearby players see the same thing.

The mod ships exactly **one custom packet**, for the unibeam, because a key press is the one piece
of state the server genuinely cannot observe. It carries no data: the client only reports that the
key was pressed, and the server decides on its own whether the suit is worn and charged, where the
beam points, what it hits and what it costs — so a client cannot ask for a shot it has not earned.
Everything else still rides a synced vanilla carrier rather than a bespoke packet, which would
have meant a second source of truth that could drift.

### Layout

```
src/main/java/dev/alqu/tinman/
├── advancement/   custom flight-distance criterion + persistent attachment
├── block/         Assembler and Charging Station blocks and block entities
├── component/     the tinman:energy data component
├── config/        config record, read/written as config/tinman.json
├── entity/        PulseBolt projectile
├── item/          armour, weapons, energy helpers
├── menu/          container menus (server-authoritative slot rules)
├── recipe/        the tinman:assembling recipe type and serializer
├── registry/      registration holders
├── suit/          server-side suit behaviour: flight, drain, immunities
└── worldgen/      config-driven ore placement modifier
```

`src/client/resources/tinman.client.mixins.json` holds the mod's only mixin, client-only: it feeds
the flight lean into the two render-state fields vanilla's elytra pose already reads, rather than
rotating the model itself, so the lean is the real elytra pose and stays correct if Mojang changes
how that pose is built.

`src/main/resources/tinman.accesswidener` widens exactly two methods,
`GhostSlots.setInput/setResult`. They are protected and live in a vanilla package, so a mod's own
`RecipeBookComponent` cannot fill ghost slots without it. Nothing else in the mod needs widening.

## Placeholder assets

Every texture, the five sounds and the mod icon are generated placeholders, sized and named the way
the real thing would be, so you can drop replacements straight in:

- Blocks and items: `assets/tinman/textures/{block,item}/*.png` (16×16)
- Worn armour layers: `assets/tinman/textures/entity/equipment/humanoid{,_leggings}/tin_man.png` (64×32)
- Particles: `assets/tinman/textures/particle/*.png` (8×8, 4 and 3 frame animations)
- GUIs: `assets/tinman/textures/gui/container/*.png` (256×256)
- Sounds: `assets/tinman/sounds/*.ogg` (Ogg Vorbis, mono 44.1 kHz)

`thruster_loop.ogg` and `assembler_hum.ogg` are crossfaded to loop seamlessly.

## What has and hasn't been tested

Verified by running a real dedicated 26.2 server and inspecting world data:

- The mod loads with no errors; ore generates in the right Y band at the measured rate.
- The Assembler crafts from its own recipe type, consuming the grid and the right number of
  power-cell ingots.
- Recharging works both in the Assembler and the Charging Station, at the configured rate.
- Batteries charge correctly, including enchanted ones, and Conservation reads back from the
  datapack registry at every level: a 100-energy action costs 100 / 85 / 70 / 55 at levels 0-3.
- The mixin config loads and Mixin reports the JAVA_25 compatibility level, and the injector's
  compiled descriptor matches the game's `extractRenderState` byte for byte.
- Worn on a mob and read back with `/attribute`, the suit reports 30 armour / 20 toughness /
  0.8 knockback resistance against netherite's 20 / 12 / 0.4.
- The unibeam damages every target along its line, leaves entities off the line alone, and stops
  at terrain: two mobs in line both took the hit, one off to the side and one behind a wall took
  nothing, and the trace ended at the wall rather than its full range.
- The Assembler still crafts correctly after its menu moved onto RecipeBookMenu, and the 23
  recipe-unlock advancements load (1692 -> 1715 advancements).
- A full suit in the Charging Station's four slots charges in lockstep — all four pieces read
  identical energy at every sample, at exactly the configured rate.
- Energy persists in NBT as a data component.
- Pulse bolts damage mobs, despawn on impact, and a charged blast damages mobs while leaving
  adjacent glass intact.
- A hopper → Assembler → hopper → chest chain auto-crafted four times unattended.

**Not verified**, because it needs a real graphical client rather than a headless server: the HUD
overlay, screen rendering, the Assembler's recipe book, particle appearance, sound playback, worn
armour layers, the flight lean on screen, and flight handling as felt in first person. The code paths are there, but treat the visuals and flight feel
as the first things to check in game.

A dedicated server never loads blockstates, models or textures at all, so anything wrong in those
files is invisible to the tests above — a blockstate whose variant keys don't match the block's
real property names still starts a server cleanly and only shows up in game as an untextured
block. If you touch a blockstate JSON, check its property names against the `BooleanProperty` /
`EnumProperty` declarations in the matching block class.

## Licence

GPL-3.0-or-later — see `LICENSE`.
