# Jobs

Jobs define what Xoonglins do during the day. They are assigned via the `jobs` field in
a social class definition. Each social class can list multiple jobs, and each Xoonglin
will randomly pick one from the list, which then can be changed by the player.
A Xoonglin with a job will work a configurable
number of hours per Minecraft day, tracked through a daily tick quota.

Job progress can be viewed in the **Job tab** of the Xoonglin info screen (accessed via
the leader staff).

All jobs share these optional fields:

- `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
  Xoonglin to be able to work.
- `min_happiness`: _(Optional, default: 0.0)_ The Xoonglin will not work while its
  happiness is below this threshold, resuming once it recovers.
- `available_to_babies`: _(Optional, default: false)_ Whether baby Xoonglins can be
  assigned this job. Lets a social class offer a baby-specific job (e.g. a student).
- `available_to_adults`: _(Optional, default: true)_ Whether adult Xoonglins can be
  assigned this job.
- `schedule`: _(Optional)_ A daily schedule for Xoonglins with this job, replacing the one
  of their social class (e.g. guards working the night shift). Same format as the social
  class `schedule`, see [Schedules](schedules.md).

A Xoonglin is only ever assigned a job it is eligible for at its current age, picked
randomly among its social class's `jobs` list. It is reassigned automatically whenever
that stops being true — when it grows from baby to adult, or when its social class
changes — falling back to no job if the class has no eligible job for that age.

## Home Artisan

The Xoonglin works at home and produces items periodically.

??? example "Sample home artisan job file"

    ```json
    {
      "type": "cft:home_artisan",
      "hours_per_day": 6.0,
      "frequency_days": 2,
      "output": {
        "item": "minecraft:paper"
      },
      "output_count": 2
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `frequency_days`: How many consecutive days of meeting the quota are needed before
      producing output.
    - `output`: The item to produce, in Ingredient format.
    - `output_count`: How many items to produce each cycle.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Gatherer

The Xoonglin wanders near home, breaks matching blocks, and deposits the drops in the
home container.

??? example "Sample gatherer job file"

    ```json
    {
      "type": "cft:gatherer",
      "hours_per_day": 4.0,
      "gather_radius": 16,
      "block_tag": "minecraft:flowers"
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `gather_radius`: How far from home the Xoonglin will search for blocks.
    - `block`: _(Optional)_ A specific block to gather.
    - `block_tag`: _(Optional)_ A block tag; any block in the tag will be gathered.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Guard

The Xoonglin patrols around their home and attacks hostile mobs that come nearby.

By default, monsters leave Xoonglins alone. With `monstersHuntXoonglins` enabled in the mod
config, zombies (including husks, drowned and zombie villagers) attack Xoonglins as they attack
villagers, which makes nights dangerous and guards more useful. A good fit for that is a guard job
with a `schedule` that works at night.

??? example "Sample guard job file"

    ```json
    {
      "type": "cft:guard",
      "hours_per_day": 8.0,
      "patrol_radius": 12,
      "detection_radius": 16
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to patrol each day.
    - `patrol_radius`: How far from home the Xoonglin will patrol.
    - `detection_radius`: _(Optional, default: 16)_ How far the Xoonglin can detect hostile
      mobs.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Farmer

The Xoonglin works at a farm structure, planting seeds and harvesting crops when they
are ripe. The farmer needs a farm structure (open air platform) to work at, and will
plant seeds on farmland blocks within the farm, then harvest the crop when it reaches
the ripe state.

??? example "Sample farmer job file"

    ```json
    {
      "type": "cft:farmer",
      "hours_per_day": 8.0,
      "required_structure": "cft:farm",
      "seed": {
        "item": "minecraft:wheat_seeds"
      },
      "product": {
        "item": "minecraft:wheat"
      },
      "crop_block": "minecraft:wheat",
      "ripe_state": {
        "age": "7"
      }
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: A reference to a structure type ID where the farmer will work
      (e.g. `"cft:farm"`).
    - `seed`: The seed item, in Ingredient format.
    - `product`: The harvested product, in Ingredient format.
    - `crop_block`: The block ID of the crop that grows from the seed.
    - `ripe_state`: A map of block state properties that indicate the crop is ready to harvest
      (e.g. `{"age": "7"}` for fully grown wheat).
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Hauler

The Xoonglin moves items between structures. Each errand defines an origin structure,
a destination structure and the items to transport. The hauler will take items from the
origin's container, carry them in its inventory and deposit them in the destination's
container. Carried items are visible in the **Items tab** of the Xoonglin info screen.

??? example "Sample hauler job file"

    ```json
    {
      "type": "cft:hauler",
      "hours_per_day": 6.0,
      "radius": 64,
      "errands": [
        {
          "origin_structure": "cft:farm",
          "destination_structure": "cft:settler_house",
          "items": [
            {
              "item": {
                "item": "minecraft:wheat"
              },
              "quantity": 32
            }
          ]
        }
      ]
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `radius`: How far from the Xoonglin the origin and destination structures can be.
    - `errands`: A list of transport errands. The hauler will cycle through them.
        - `origin_structure`: A reference to the structure type ID to take items from.
        - `destination_structure`: A reference to the structure type ID to deliver items to.
        - `items`: A list of items to transport.
        - `item`: The item to transport, in Ingredient format.
        - `quantity`: How many items to move per trip.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Builder

The Xoonglin builds new structures by replicating existing ones. **The player must place
the key block of a buildable structure (e.g. the door of a house) where the new building
should stand**: the builder looks for key blocks within its build radius that are not
part of any already detected structure, and takes them as build sites. It then finds the
closest detected structure of the same type belonging to the same leader, uses it as a
template, and replicates it block by block around the new key block.

Building materials are taken from the container of the storage structure. If
`storage_structure` is the same as `required_structure`, the builder only takes materials
from the structure it is a user of; otherwise it uses the closest storage structure it
is a user of. If materials run out, the builder waits by the container until the player
restocks it.

Note that the newly built structure still needs to be detected with the leader staff to
be recognized by the mod.

??? example "Sample builder job file"

    ```json
    {
      "type": "cft:builder",
      "hours_per_day": 8.0,
      "storage_structure": "cft:settler_house",
      "build_radius": 48,
      "buildable_structures": [
        "cft:settler_house"
      ]
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: _(Optional)_ A reference to a structure type ID the builder must
      be a user of to work. If omitted, no structure is required.
    - `storage_structure`: A reference to the structure type ID whose container provides the
      building materials.
    - `build_radius`: _(Optional, default: 64)_ How far from the builder new key blocks are
      searched for.
    - `build_speed`: _(Optional, default: 20)_ Ticks between placing one block and the next.
    - `buildable_structures`: A list of structure type IDs the builder knows how to build.
      For each of them, at least one detected structure of the same type must exist to serve
      as template.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Officiant

The Xoonglin periodically performs a ritual at the key block of its required structure.
Rituals are the counterpart of the **Ritual need**: when the ritual completes, all
Xoonglins with a matching ritual need nearby get it satisfied. As with the need, rituals
don't have to be religious — any recurring ceremony, celebration or communal event fits.

The officiant goes to the structure, checks that the ingredients are available in its
container, and summons attendees of the required social classes. The ritual starts once
the attendance minimums are met (after a short grace period to let more attendees
arrive, up to the maximum). If the minimums are not met before the gathering timeout,
the ritual is postponed and retried later. During the ritual the officiant stays by the
key block while the attendees stand around watching. Rituals survive world reloads and
resume where they left off.

??? example "Sample officiant job file"

    ```json
    {
      "type": "cft:officiant",
      "ritual_id": "cft:communion",
      "required_structure": "cft:temple",
      "frequency": 1.0,
      "duration": 600,
      "ingredients": [
        {
          "item": {
            "item": "minecraft:wheat"
          },
          "quantity": 4
        }
      ],
      "summon_radius": 48,
      "ritual_radius": 8,
      "attendance": [
        {
          "classes": ["cft:settler", "cft:citizen"],
          "min": 1
        }
      ],
      "max_attendees": 20,
      "gathering_timeout": 1200,
      "grace_period": 200
    }
    ```
    - `ritual_id`: Identifier of the ritual this officiant performs. Ritual needs with the
      same `ritual_id` are satisfied when the ritual completes.
    - `required_structure`: A reference to the structure type ID where the ritual takes
      place. The officiant must be a user of one, and performs the ritual at its key block.
    - `frequency`: How often the ritual is performed, in Minecraft days.
    - `duration`: How long the ritual lasts, in ticks (20 ticks = 1 second).
    - `ingredients`: _(Optional)_ A list of items consumed from the structure's container
      when the ritual starts.
        - `item`: The ingredient, in Ingredient format.
        - `quantity`: How many are consumed per ritual.
    - `summon_radius`: _(Optional, default: 32)_ How far the officiant looks for Xoonglins
      to summon as attendees.
    - `ritual_radius`: _(Optional, default: 8)_ Attendees must stand within this distance of
      the key block during the ritual.
    - `attendance`: _(Optional)_ A list of attendance rules that must be met for the ritual
      to start. A Xoonglin counts toward every rule that lists its social class, so rules
      can express requirements like "at least 10 settlers or citizens, and at least 2
      patricians".
        - `classes`: A list of social class IDs this rule applies to.
        - `min`: _(Optional, default: 0)_ Minimum number of Xoonglins of these classes.
        - `max`: _(Optional, default: unlimited)_ Maximum number of Xoonglins of these classes.
    - `max_attendees`: _(Optional, default: unlimited)_ Maximum total number of attendees.
    - `gathering_timeout`: _(Optional, default: 1200)_ Ticks to wait for the attendance
      minimums before postponing the ritual.
    - `grace_period`: _(Optional, default: 200)_ Once the minimums are met, ticks to wait
      for more attendees before starting.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Crafter

The Xoonglin works at a workshop structure, consuming ingredients from the workshop's
container to craft an output that is deposited back into the same container. Unlike the
home artisan, the inputs of the craft are explicit: crafting only happens while the
container holds the ingredients, which must be delivered to the workshop (for example
by haulers). This allows building production chains: a farmer grows wheat, a hauler
brings it to the bakery, the baker turns it into bread, and another hauler distributes
the bread to the homes.

If the ingredients run out, the crafter waits at the workshop and rechecks periodically.

??? example "Sample crafter job file"

    ```json
    {
      "type": "cft:crafter",
      "hours_per_day": 8.0,
      "required_structure": "cft:bakery",
      "ingredients": [
        {
          "item": {
            "item": "minecraft:wheat"
          },
          "quantity": 3
        }
      ],
      "output": {
        "item": "minecraft:bread"
      },
      "output_count": 1,
      "crafting_time": 200
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: A reference to the structure type ID of the workshop. The
      crafter must be a user of one, and works at its key block.
    - `ingredients`: A list of items consumed from the workshop container for each craft.
        - `item`: The ingredient, in Ingredient format.
        - `quantity`: How many are consumed per craft.
    - `output`: The crafted item, in Ingredient format.
    - `output_count`: _(Optional, default: 1)_ How many items are produced per craft.
    - `crafting_time`: _(Optional, default: 200)_ How many ticks each craft takes.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Enchanter

The Xoonglin works at an enchanting structure, taking an item from the structure's
container and applying an enchantment from its repertoire, depositing it back into the
same container afterward. Unlike the crafter, there's no separate output: the item
itself is enchanted in place, so it must already be something enchantments can apply to
(e.g. tools, weapons, armor).

Each time it works, the enchanter looks across the container for an item matching
`input`, then checks its `repertoire` for an enchantment that: is compatible with the
item, is compatible with whatever enchantments are already on it (following the same
exclusivity rules as an enchanting table, e.g. Sharpness and Smite can't coexist), and
isn't already present at its configured maximum level. If it finds one, it applies it at
a random level within the configured range (clamped to the enchantment's own maximum
level). If nothing eligible is found, it waits at the structure and rechecks
periodically.

??? example "Sample enchanter job file"

    ```json
    {
      "type": "cft:enchanter",
      "hours_per_day": 6.0,
      "required_structure": "cft:enchanting_room",
      "input": {
        "tag": "minecraft:swords"
      },
      "repertoire": [
        { "enchantment": "minecraft:sharpness", "min_level": 1, "max_level": 5 },
        { "enchantment": "minecraft:knockback", "min_level": 1, "max_level": 2 }
      ],
      "enchanting_time": 200
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: A reference to the structure type ID of the enchanting
      structure. The enchanter must be a user of one, and works at its key block.
    - `input`: The item eligible to be enchanted, in Ingredient format.
    - `repertoire`: A list of enchantments the enchanter can apply.
        - `enchantment`: The enchantment's ID.
        - `min_level` / `max_level`: _(Optional, default: 1)_ The level range to pick from,
        clamped to the enchantment's own maximum level.
    - `enchanting_time`: _(Optional, default: 200)_ How many ticks each enchantment takes to
      apply.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Fisher

The Xoonglin looks for a body of blocks — water by default, but configurable so modded
fluids can be fished too — of at least a minimum size within its radius, stands at the
water's edge and fishes. If a `required_structure` is set (e.g. a dock), the fisher
stands on the structure's own blocks, as close to the water as possible without leaving
it; otherwise it stands on the shore.

While fishing, it rolls the weighted catch list periodically. **The weights must sum 1
or less**: the missing probability is the chance that nothing bites on that try.
Catches are carried in the Xoonglin's inventory (visible in the Items tab) and, at the
end of the work day, deposited in the required structure's containers — or in its home
container if there is no structure.

??? example "Sample fisher job file"

    ```json
    {
      "type": "cft:fisher",
      "hours_per_day": 6.0,
      "radius": 32,
      "min_body_size": 40,
      "catch_interval": 300,
      "catches": [
        {
          "item": {
            "item": "minecraft:cod"
          },
          "weight": 0.5
        },
        {
          "item": {
            "item": "minecraft:salmon"
          },
          "weight": 0.25
        },
        {
          "item": {
            "item": "minecraft:pufferfish"
          },
          "weight": 0.05
        }
      ]
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `radius`: _(Optional, default: 32)_ How far from the structure (or from the fisher)
      the body of blocks is searched for.
    - `required_structure`: _(Optional)_ A reference to a structure type ID the fisher must
      be a user of and stand on while fishing (e.g. a dock).
    - `body_blocks`: _(Optional, default: water)_ A list of blocks that form the fished
      body. Each entry has a `block` or a `tagBlock` field.
    - `min_body_size`: _(Optional, default: 20)_ Minimum number of connected body blocks
      for a body to be fishable.
    - `catch_interval`: _(Optional, default: 300)_ Ticks between catch attempts.
    - `catches`: The weighted list of possible catches.
        - `item`: The caught item, in Ingredient format.
        - `weight`: Probability of this catch per attempt. All weights must sum 1 or less.
        - `count`: _(Optional, default: 1)_ How many items are caught at once.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Healer

The Xoonglin heals nearby damaged Xoonglins (and optionally the leader) by spending
items. If a `required_structure` is set, the healer works from it — treating patients
within a radius of the structure and drawing supplies from its containers; otherwise it
works from its home. Supplies are carried in the healer's inventory and restocked from
the base as needed.

Each heal restores a configurable amount of health, consumes one set of the configured
items, and is subject to a cooldown. If `items` is empty, healing is free.

??? example "Sample healer job file"

    ```json
    {
      "type": "cft:healer",
      "hours_per_day": 8.0,
      "radius": 24,
      "items": [
        {
          "item": {
            "item": "minecraft:golden_carrot"
          },
          "quantity": 1
        }
      ],
      "heal_amount": 6.0,
      "cooldown": 100,
      "heal_player": true
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `radius`: _(Optional, default: 24)_ How far from the base patients are searched for.
    - `required_structure`: _(Optional)_ A reference to a structure type ID the healer works
      from and takes supplies from. If omitted, the healer works from its home.
    - `items`: _(Optional)_ A list of items consumed per heal. Taken from the base container.
        - `item`: The item, in Ingredient format.
        - `quantity`: How many are consumed per heal.
    - `heal_amount`: How much health each heal restores (2.0 = one heart).
    - `cooldown`: _(Optional, default: 100)_ Ticks between heals.
    - `doses_per_fetch`: _(Optional, default: 16)_ How many heals' worth of items the healer
      grabs from the base on each restock trip.
    - `heal_player`: _(Optional, default: false)_ Whether the healer also heals the leader
      when they are damaged and within the radius.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Blesser

The Xoonglin blesses nearby Xoonglins (and optionally the leader) by spending items,
applying a set of beneficial effects. If a `required_structure` is set, the blesser
works from it — treating targets within a radius of the structure and drawing supplies
from its containers; otherwise it works from its home. Supplies are carried in the
blesser's inventory and restocked from the base as needed.

A target is picked whenever it's missing at least one of the configured effects. Each
blessing applies all of them at once, consumes one set of the configured items, and is
subject to a cooldown. If `items` is empty, blessing is free.

??? example "Sample blesser job file"

    ```json
    {
      "type": "cft:blesser",
      "hours_per_day": 1.0,
      "radius": 24,
      "items": [
        {
          "item": {
            "item": "minecraft:nether_wart"
          },
          "quantity": 1
        }
      ],
      "effects": [
        { "effect": "minecraft:strength", "duration": 6000, "amplifier": 0 },
        { "effect": "minecraft:speed", "duration": 6000, "amplifier": 0 }
      ],
      "cooldown": 100,
      "bless_player": true
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `radius`: _(Optional, default: 24)_ How far from the base targets are searched for.
    - `required_structure`: _(Optional)_ A reference to a structure type ID the blesser works
      from and takes supplies from. If omitted, the blesser works from its home.
    - `items`: _(Optional)_ A list of items consumed per blessing. Taken from the base
      container.
        - `item`: The item, in Ingredient format.
        - `quantity`: How many are consumed per blessing.
    - `effects`: A list of effects applied together on each blessing.
        - `effect`: The mob effect's ID (e.g. `minecraft:strength`).
        - `duration`: How long the effect lasts, in ticks (20 ticks = 1 second).
        - `amplifier`: _(Optional, default: 0)_ The effect's amplifier (0 = level I).
    - `cooldown`: _(Optional, default: 100)_ Ticks between blessings.
    - `doses_per_fetch`: _(Optional, default: 16)_ How many blessings' worth of items the
      blesser grabs from the base on each restock trip.
    - `bless_player`: _(Optional, default: false)_ Whether the blesser also blesses the
      leader when they are missing an effect and within the radius.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Rancher

The Xoonglin tends the animals of a pasture: shears sheep, milks cows, and feeds pairs of
eligible animals to trigger breeding. Each tick it looks across the whole pasture
footprint for the nearest ready action among the three (whichever is enabled and off
cooldown), travels to it, and performs it. Wool and milk are deposited in the pasture's
container; feed is taken from it — so the pasture should declare
`"requires_container": true` for the rancher to actually be able to restock and deposit.

Shearing and milking are on their own cooldowns and don't need `feed` configured, so a
rancher can be set up for only some of the three actions (e.g. a pure shepherd with
`"milk": false`). Feeding picks any two adult, breeding-ready animals of the same species
within the pasture where a configured `feed` item is valid food for them (checked via the
animal's own species-specific food rules, e.g. wheat for sheep and cows, carrots for
pigs), consumes one unit of that item per animal, and lets them breed on their own —
exactly as if a player had fed them by hand.

Shearing requires a pair of shears carried by the rancher (fetched from the container
like any other supply, and worn down with use — a broken pair is discarded and must be
replaced). Milking only requires an empty bucket, but since vanilla cows have no
cooldown of their own, each cow gets a `milk_regen_ticks` cooldown tracked individually
so the rancher doesn't just repeatedly milk the same cow.

??? example "Sample rancher job file"

    ```json
    {
      "type": "cft:rancher",
      "hours_per_day": 4.0,
      "required_structure": "cft:pasture",
      "feed": [
        {
          "item": {
            "item": "minecraft:wheat"
          },
          "quantity": 1
        }
      ],
      "shear": true,
      "milk": true,
      "action_cooldown": 200
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: A reference to the pasture structure type ID. The rancher must be
      a user of one.
    - `feed`: _(Optional)_ A list of items that can be fed to trigger breeding. Each entry's
      `quantity` is consumed per animal (so a pair costs twice that). If empty, breeding is
      disabled.
        - `item`: The item, in Ingredient format.
        - `quantity`: How many are consumed per animal fed.
    - `shear`: _(Optional, default: true)_ Whether the rancher shears eligible sheep.
    - `milk`: _(Optional, default: true)_ Whether the rancher milks cows (requires empty
      buckets in the container).
    - `action_cooldown`: _(Optional, default: 200)_ Ticks between individual actions (shears,
      milkings, or feedings each have their own cooldown of this length).
    - `doses_per_fetch`: _(Optional, default: 16)_ How many feedings' worth of items the
      rancher grabs from the container on each restock trip.
    - `buckets_per_fetch`: _(Optional, default: 4)_ How many empty buckets the rancher grabs
      from the container on each restock trip.
    - `milk_regen_ticks`: _(Optional, default: 6000)_ Minimum time between milkings of the
      same cow.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Writer

The Xoonglin periodically writes an original book, or **manuscript**: a title, author
(the Xoonglin's own name) and pages are generated procedurally, and the book is
registered in the world's shared **roster** of titles, then deposited in the base container.

Unlike most production jobs, writing a book takes several days of accumulated work
rather than being repeatable within a single day — `frequency_days` sets how many
consecutive full workdays are needed before a book is produced (modeled on the Home
Artisan job's cadence). If `required_structure` is omitted, the writer works from home,
like the Healer/Blesser jobs; otherwise it works at the given structure.

Each leader's roster is capped at `needs.maxRosterSize` in the mod config (default 20).
If the roster is already full, the writer simply holds at its production threshold —
work isn't lost — and writes the book as soon as room frees up (e.g. an existing entry's
only physical copies are all lost, or the cap is raised).

If `input` is configured — for example, a book and quill — the writer first fetches it
from the base container into its own inventory, and only counts a day's work toward the
streak while it's actually holding it: no input, no progress, and it'll go fetch more
before resuming. The input is consumed (and the book deposited) only once the streak is
actually reached, so partial progress is never lost to a missing supply.

The words and sentence shapes a writer draws from are entirely datapack-configurable via
`text_bank`, so different writer jobs can produce recognizably different books — a
useful hook for e.g. giving each in-game "culture" its own vocabulary, or even its own
language. Text is generated by picking a random template (a plain string) and filling
each `%category%` placeholder with a random word from the matching list; the categories
are `%noun%`, `%adjective%`, `%verb%` and `%place%`. A placeholder for an unknown or
empty category is left in the output as-is, rather than crashing generation — useful for
spotting a typo in a custom `text_bank`. If `text_bank` is omitted entirely, a built-in
English default is used.

??? example "Sample writer job file"

    ```json
    {
      "type": "cft:writer",
      "hours_per_day": 2.0,
      "frequency_days": 10,
      "pages_per_book": 6,
      "input": {
        "item": {
          "item": "minecraft:writable_book"
        },
        "quantity": 1
      },
      "text_bank": {
        "nouns": ["miller", "lantern", "hollow", "raven"],
        "adjectives": ["quiet", "restless", "hollow", "distant"],
        "verbs": ["spoke of", "waited for", "forgot", "carried"],
        "places": ["the valley", "the old road", "the harbor town"],
        "title_templates": [
          "The %noun% of the %adjective% %place%",
          "A %adjective% %noun%"
        ],
        "sentence_templates": [
          "The %adjective% %noun% %verb% the %adjective% %noun% in %place%.",
          "%noun% was never %adjective% in %place%."
        ]
      }
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `frequency_days`: How many consecutive full workdays are needed to produce one book.
    - `required_structure`: _(Optional)_ A reference to a structure type ID the writer works
      from and deposits into. If omitted, the writer works from home.
    - `pages_per_book`: _(Optional, default: 6)_ How many pages the generated book has.
    - `input`: _(Optional)_ An item consumed from the base container each time a book is
      produced (e.g. a book and quill). If omitted, writing is free.
        - `item`: The item, in Ingredient format.
        - `quantity`: How many are consumed per book.
    - `text_bank`: _(Optional, default: built-in English word bank)_ The word lists and
      templates used to generate this writer's books.
        - `nouns` / `adjectives` / `verbs` / `places`: Lists of words for each category.
        - `title_templates`: Templates for the book's title, filled with `%category%`
        placeholders. One is picked at random per book.
        - `sentence_templates`: Templates for individual sentences within a page, filled the
        same way. Picked repeatedly at random to fill each page.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Scribe

The Xoonglin works at a scriptorium, copying books. It looks in the structure's
container for any book carrying a valid manuscript/copy from the current leader's roster
that isn't already at the maximum copyable generation, and — given enough of the
configured input — spends some time producing a new copy of it (one generation further
from the original; vanilla copies cap at generation 2, "tattered" copies at generation 3
can no longer be copied). **The source book is never consumed**, so the scriptorium can
keep producing copies from the same manuscript over time; only the input is spent. Like
the Writer job, the scribe fetches the input into its own inventory before working, and
only consumes it once a copy is actually produced.

??? example "Sample scribe job file"

    ```json
    {
      "type": "cft:scribe",
      "hours_per_day": 6.0,
      "required_structure": "cft:scriptorium",
      "input": {
        "item": {
          "item": "minecraft:paper"
        },
        "quantity": 3
      },
      "crafting_time": 400
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: A reference to the scriptorium structure type ID. The scribe must
      be a user of one.
    - `input`: The item consumed per copy, in the same format as a crafter's ingredients.
        - `item`: The item, in Ingredient format.
        - `quantity`: How many are consumed per copy.
    - `crafting_time`: _(Optional, default: 200)_ How many ticks each copy takes.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Trader

A player-facing shopkeeper with its own simple trading system (not vanilla's
Merchant/villager trades). The leader configures up to `max_trades` item-for-item
exchanges at runtime, from the "Configure Trades" button on the Job tab — not via
datapack. At the start of each work day the trader hoards as much as it can of each
configured trade's "given" item from its base container. Any player who is **not** the
trader's leader can right-click it to open a trade list and exchange items directly with
its inventory. At the end of the work day, the trader deposits everything it's carrying
(unsold stock and payment received) back into the base container; whatever doesn't fit
stays in its inventory into the next day.

??? example "Sample trader job file"

    ```json
    {
      "type": "cft:trader",
      "hours_per_day": 6.0,
      "max_trades": 4
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: _(Optional)_ A structure type ID to use as the trader's base
      instead of its home.
    - `max_trades`: _(Optional, default: 4)_ How many trade slots the leader can configure.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

## Quarry Miner

The Xoonglin digs a quarry — an open-air platform structure marking the footprint — layer
by layer, top-down, down to a maximum depth. It maintains a ladder column at one edge of
the quarry so it can climb back to the surface, and it deposits the mined blocks in the
quarry's container. Ladders are taken from that same container, so the player must keep it
stocked with ladders.

The quarry is defined with the **open-air platform** structure type: a ring of blocks
forms the perimeter, the surface inside it is the top layer to be dug, and the container
sits in the perimeter ring. The miner never digs outside the footprint, so the
surrounding terrain forms the quarry walls (and backs the ladder column).

If the working layer, or any layer above it, is flooded by a fluid, the miner stops and
the job status shows **Quarry Flooded**. The player must drain or seal it manually; the
miner rechecks periodically and resumes once it is dry.

??? example "Sample quarry miner job file"

    ```json
    {
      "type": "cft:quarry_miner",
      "hours_per_day": 8.0,
      "required_structure": "cft:quarry",
      "max_depth": 16,
      "mine_speed": 20
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: A reference to the quarry structure type ID. The miner must be a
      user of one.
    - `max_depth`: _(Optional, default: 16)_ How many layers down the quarry is dug.
    - `mine_speed`: _(Optional, default: 20)_ Ticks between mining one block and the next.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.



## Smelter

The Xoonglin works at a workshop with vanilla furnaces, smokers or blast furnaces, and keeps them
running: every now and then it collects their results into the workshop's chests and loads them
with things to cook and fuel taken from those same chests. The furnaces do the actual cooking, so
recipes, fuel values and cooking times are vanilla's, and any item they can cook works (a smoker
only takes food, a blast furnace only ores and metal, as usual).

Only the three vanilla furnace types are tended; furnaces added by other mods are left alone.
A cook is the same job at a structure with smokers, like the bakery, with its `inputs` limited
to raw food.

??? example "Sample smelter job file"

    ```json
    {
      "type": "cft:smelter",
      "hours_per_day": 8.0,
      "required_structure": "cft:smeltery"
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: A reference to the structure type ID of the workshop. The smelter must
      be a user of one, and works at its key block.
    - `inputs`: _(Optional)_ A list of items, in Ingredient format, that the smelter may put in the
      furnaces. If empty or omitted, anything the furnaces can cook.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.

??? example "Sample cook job file"

    ```json
    {
      "type": "cft:smelter",
      "hours_per_day": 6.0,
      "required_structure": "cft:bakery",
      "inputs": [
        { "item": "minecraft:beef" },
        { "item": "minecraft:porkchop" },
        { "item": "minecraft:potato" }
      ]
    }
    ```

## Lumberjack

The Xoonglin fells whole trees around its base: a lumber camp if the job has a
`required_structure`, or its home otherwise. It walks to the nearest tree and chops the base of its
trunk until the whole tree falls.

It needs an axe.

??? example "Sample lumberjack job file"

    ```json
    {
      "type": "cft:lumberjack",
      "hours_per_day": 6.0,
      "required_structure": "cft:lumber_camp",
      "chop_radius": 16,
      "max_tree_size": 128
    }
    ```
    - `hours_per_day`: How many Minecraft hours the Xoonglin needs to work each day.
    - `required_structure`: _(Optional)_ A structure type ID to use as the lumberjack's base, like
      a lumber camp. Without it, it works around its home and uses the home's containers.
    - `chop_radius`: _(Optional, default: 16)_ How far from its base it looks for trees.
    - `max_tree_size`: _(Optional, default: 128)_ Log clusters bigger than this are not felled.
    - `required_needs`: _(Optional)_ A list of need IDs that must be satisfied for the
      Xoonglin to be able to work.
