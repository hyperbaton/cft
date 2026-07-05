# Care For Them
Care For Them is a Minecraft mod that introduces the Xoonglin, a new mob which players
need to provide for. Xoonglins have a multitude of needs that, once satisfied, will
give them plenty of happiness. However, if they can't satisfy them, they will become
unhappy and could even die.

## Rationale

The original idea for this mod came from the realization that many mods and modpacks
are oriented into creating large factories full of processes for producing lots of
resources, but there are very few ways to spend those products. The needs in the
Minecraft world are very limited, and can be satisfied with just a bit of manual
labour. For big factories to make sense, we also need a big population that consumes
whatever comes from the factories.

## Features

- Xoonglins, new mobs that will expect the player to provide for their needs.
- Xoonglins have **needs**, which can be of different kinds. They always need a physical
home where to live and some items to be periodically delivered to their homes.
- Each Xoonglin belongs to a **social class**. Each of these have a different set of social
needs. If their needs are satisfied and their happiness increases, they can upgrade to
higher classes; however, if they get unhappy, they can demote to a lower class.
- The **leader staff** can be used to designate a home (by right clicking on doors while
crouching), detect structures (by right clicking on a structure's key block) or know
current state of a Xoonglin.
- The first Xoonglins will spawn spontaneously, but from then on, they will mate to increase
their population. However, they will always respect a given social structure (a relation
on the amount of Xoonglins of each class).
- You can compete with other players for getting the biggest and happiest Xoonglin
population. The commands `\happinessLadder`, `\populationLadder` and `\socialstructure` give
rankings and information on your Xoonglins.
- The social classes, needs, structures and jobs are fully configurable and customizable via
datapacks, so it's possible to build a tailored experience for any modpack.
- **Social Class Browser**: Press `V` to open an interactive screen showing the full social
class hierarchy as a visual graph, with clickable nodes to view each class's needs,
stats, and upgrade/downgrade conditions.
- **Jobs**: Xoonglins can be assigned jobs through their social class. Each social class can
have multiple possible jobs, and each Xoonglin will randomly pick one. The player can manually
change the job of a Xoonglin among the available ones.
- **Structures**: Buildings in the world can be recognized and validated by the mod. Homes
are a special type of structure, but other structure types (workshops, farms, monuments,
multi-storey buildings) can also be defined and used by jobs and needs.
- **Rituals**: Xoonglins with the officiant job can perform rituals — or any kind of
configurable ceremony or event — that other Xoonglins attend and get happiness from.
- **Equipment**: Xoonglins can wear items and armor in any equipment slot, driven by
equipment needs.

## Datapacks

It is possible to configure the social classes, needs, structures and jobs of Xoonglins
via datapacks, and any aspect of them can be configured.

Some sample classes and needs come packaged with the mod, but they are only intended
as examples for the possibilities of the mod. It is strongly advised to create a
datapack with specific classes for a given modpack.

The datapack documentation is presented below.

### Social Class

<details>
    <summary>Sample social class file</summary>

```json
{
  "id": "cft:settler",
  "maxHappiness": 100.0,
  "matingHappinessThreshold": 5.0,
  "spontaneouslySpawnPopulation": 3,
  "needs": [
    "cft:settler_home",
    "cft:water_need",
    "cft:potato_need",
    "cft:wood_logs_need",
    "cft:bread_need"
  ],
  "jobs": ["cft:wheat_farmer_job", "cft:gather_flowers_job"],
  "upgrades": [
    {
      "nextClass": "cft:citizen",
      "requiredHappiness": 20,
      "requiredNeeds": [
        {
          "need": "cft:potato_need",
          "satisfactionThreshold": 0.70
        }
      ],
      "socialStructureRequirements": [
        {
          "socialClass": "cft:settler",
          "percentage": 0.30
        }
      ]
    }
  ],
  "downgrades": []
}
```

</details>

- `id`: Identifier of the social class.
- `maxHappiness`: Happiness for a single Xoonglin of this class will not get greater
  than this.
- `matingHappinessThreshold`: The happiness value a Xoonglin needs to achieve to consider
  mating. Extra conditions may apply.
- `spontaneouslySpawnPopulation`: The number of individuals of this class that will
  spawn (per player) if homes are available. Apart from these, they need to mate or come
  from other classes.
- `needs`: The list of needs, as references, for this class.
- `jobs`: _(Optional)_ A list of job references for this class. Each Xoonglin will randomly
  pick one of the listed jobs. If empty or omitted, the Xoonglin has no job.
- `maxHealth`: _(Optional, default: 20.0)_ The max health for Xoonglins of this class.
  Useful for making combat-oriented classes tougher.
- `canUpgradeAsBaby`: _(Optional, default: false)_ Whether baby Xoonglins of this class
  can upgrade.
- `canDowngradeAsBaby`: _(Optional, default: true)_ Whether baby Xoonglins of this class
  can downgrade.
- `matingDelay`: _(Optional, default: -1)_ Custom mating cooldown in ticks for this class.
  If -1, uses the global config value.
- `upgrades`: A list of ways a Xoonglin can become a higher class.
    - `nextClass`: Reference to next class.
    - `requiredHappiness`: Minimum happiness level to consider upgrading.
    - `requiredNeeds`: These needs have to be satisfied at the given value for the upgrade
      to be possible.
    - `socialStructureRequirements`: A list of such requirements. Each social class mentioned
      must represent a percentage lower or equal to this one. Always in the range [0,1].
- `downgrades`: A list of ways a Xoonglin can become a lower class.
    - `nextClass`: Reference to next class.
    - `requiredHappiness`: If happiness gets lower than this, the Xoonglin will downgrade.
    - `requiredNeeds`: These needs have to be satisfied at the given value or the Xoonglin will
      downgrade.
    - `socialStructureRequirements`: A list of such requirements. Each social class mentioned
      must represent a percentage higher or equal to this one. Always in the range [0,1].

### Needs

All needs have some basic fields: `id`, `type`, `damage`, `damage_threshold`, `satisfaction_threshold`,
`frequency` and `provided_happiness`. Then depending on the `type`, they might have extra
fields and they will work differently.

All needs are checked every second for all Xoonglins. They have an internal value of
satisfaction that goes from 0 to 1. If it's above the `satisfaction_threshold`, it is
considered satisfied. In that case, happiness is increased and satisfaction is reduced.
If it is unsatisfied, Xoonglin will try to initiate
some action for satisfying the need. If it fails, happiness will decrease.

#### Goods Need

A Xoonglin needs to consume some item or block for this need to be satisfied. They will
try to get the goods from the container within their home. When resupplying, the Xoonglin
will take up to the `hoarding` amount from the container so it doesn't need to resupply
as often.

<details>
    <summary>Sample goods need file</summary>

```json
{
  "id": "cft:bread_need",
  "type": "cft:goods",
  "damage": 0.5,
  "damage_threshold": 0.5,
  "provided_happiness": 10,
  "satisfaction_threshold": 0.75,
  "item": {
    "item": "minecraft:bread"
  },
  "frequency": 0.5,
  "quantity": 2,
  "hoarding": 10
}
```

- `id`: Identifier of this need.
- `type`: Must be `"cft:goods"` to indicate this is a goods need.
- `damage`: Amount of damage per second if the need is unsatisfied.
- `damage_threshold`: The Xoonglin will receive damage if satisfaction falls below this level.
- `provided_happiness`: Happiness provided per Minecraft day (20 real minutes) if the need
is satisfied.
- `satisfaction_threshold`: If satisfaction is above this level, the need is considered
_satisfied_.
- `item`: The needed item, in Ingredient format. It can be just a reference to some item or have more complex structure,
like checking NBT tags.
- `frequency`: In Minecraft days, how long it takes for the satisfaction of this need to
go from 1 to 0.
- `hidden`: _(Optional, default: false)_ Whether this need should be hidden from interfaces.
- `quantity`: How many items of the specified type are consumed each time the need is satisfied.
- `hoarding`: _(Optional)_ How many items the Xoonglin will take from the container when
resupplying. If omitted or set to 0, defaults to `quantity`. Setting this higher than
`quantity` means the Xoonglin will stock up and won't need to visit the container as often.
</details>

#### Home Need

A Xoonglin needs a home of a specific structure type. The home need references a house
structure type (defined in the Structures section), which specifies the building rules.

Once built, right clicking with the **Leader Staff** on the door of a house will check
if it's valid. A message will appear in chat informing if it is or not.

<details>
    <summary>Sample home need file</summary>

```json
{
  "id": "cft:settler_home",
  "type": "cft:home",
  "damage": 0.0,
  "damage_threshold": 0.0,
  "provided_happiness": 8,
  "satisfaction_threshold": 0.95,
  "frequency": 1.0,
  "required_structure": "cft:settler_house"
}
```

For the common fields, look at the goods need example. The specific field for home needs is:

- `required_structure`: A reference to a house structure type ID (e.g. `"cft:settler_house"`).
  The Xoonglin's home must match this structure type to satisfy the need.
</details>

#### Equipment Need

A Xoonglin needs to wear a specific item in an equipment slot. The Xoonglin will look for
the item in its inventory and equip it automatically. If the item is not available, the
Xoonglin will try to get it from the container in its home. Equipment items take damage
over time and will eventually need to be replaced.

<details>
    <summary>Sample equipment need file</summary>

```json
{
  "id": "cft:citizen_iron_sword_need",
  "type": "cft:equipment",
  "damage": 0.0,
  "damage_threshold": 0.0,
  "provided_happiness": 0.0,
  "satisfaction_threshold": 0.5,
  "item": {
    "item": "minecraft:iron_sword"
  },
  "frequency": 7,
  "hidden": true,
  "slot": "mainhand"
}
```

For the common fields, look at the goods need example. The specific fields for equipment needs are:

- `item`: The equipment item, in Ingredient format.
- `slot`: _(Optional, default: "mainhand")_ The equipment slot where the item should be worn.
  Valid values: `"mainhand"`, `"offhand"`, `"head"`, `"chest"`, `"legs"`, `"feet"`.
</details>

#### Structure Need

A Xoonglin needs access to a specific structure type nearby. This is used when a Xoonglin
requires a workplace or facility (e.g. a smithy) without it being their home.

<details>
    <summary>Sample structure need file</summary>

```json
{
  "type": "cft:structure",
  "id": "cft:smithy_access_need",
  "damage": 0.0,
  "damage_threshold": 0.0,
  "provided_happiness": 5.0,
  "satisfaction_threshold": 0.9,
  "frequency": 1.0,
  "hidden": false,
  "required_structure": "cft:smithy",
  "requires_usage": true,
  "search_radius": 64
}
```

For the common fields, look at the goods need example. The specific fields for structure needs are:

- `required_structure`: A reference to a structure type ID that the Xoonglin needs access to.
- `requires_usage`: _(Optional, default: false)_ If true, the Xoonglin must actively visit
  and interact with the structure; if false, the structure just needs to exist nearby.
- `search_radius`: _(Optional, default: 64)_ How far (in blocks) the Xoonglin will search
  for the required structure.
</details>

#### Altitude Need

The Xoonglin needs to live in some altitude range (given by Y coordinate).

<details>
    <summary>Sample altitude need file</summary>

```json
{
  "id": "cft:high_altitude_need",
  "type": "cft:altitude",
  "damage": 0.4,
  "damage_threshold": 0.5,
  "provided_happiness": 5,
  "satisfaction_threshold": 0.75,
  "frequency": 0.1,
  "min_altitude": 100,
  "max_altitude": 300
}
```
Apart from the common fields, this need includes an altitude range:
- `min_altitude`: The minimum altitude the Xoonglin must be at any moment for the need to be satisfied.
- `max_altitude`: The altitude below which the Xoonglin must be at any moment for the need to be satisfied.
</details>

#### Biome Need

The Xoonglin needs to live in a given biome (or a set of biomes).

<details>
    <summary>Sample biome need file</summary>

```json
{
  "id": "cft:mountain_biomes_need",
  "type": "cft:biome",
  "damage": 0.4,
  "damage_threshold": 0.5,
  "provided_happiness": 5,
  "satisfaction_threshold": 0.75,
  "frequency": 0.1,
  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:windswept_forest",
    "minecraft:windswept_savanna",
    "minecraft:meadow",
    "minecraft:cherry_grove",
    "minecraft:grove",
    "minecraft:snowy_slopes",
    "minecraft:frozen_peaks",
    "minecraft:jagged_peaks",
    "minecraft:stony_peaks"
  ]
}
```
Apart from the common fields, this need includes a list of biomes:
- `biomes`: A list of biomes. The Xoonglin must be in any of them for the need to be satisfied.
</details>

#### Fluid Need

It's similar to the Goods Need, but in this case the goods are fluids that are taken from a container within the home
of the Xoonglin. Enough fluid must be present there; if it is, the Xoonglin will go to it to retrieve the fluid and
satisfy the need.

<details>
    <summary>Sample fluid need file</summary>

```json
{
  "id": "cft:water_need",
  "type": "cft:fluid",
  "damage": 0.5,
  "damage_threshold": 0.6,
  "provided_happiness": 2.0,
  "satisfaction_threshold": 0.8,
  "frequency": 1.0,
  "fluid_stack": {
    "FluidName": "minecraft:water",
    "Amount": 1000
  }
}
```
Apart from the common fields, this need includes a fluid stack object. Yes, it's in PascalCase because it uses NeoForge
parsing method for FluidStack.
- `fluid_stack`: A FluidStack object that contains the reference of the fluid and the amount in millibuckets.
</details>

#### Energy Need

Works very similar to the Fluid Needs, but in this case the product consumed is just NeoForge Energy. It can be taken from
any block within the Xoonglin's home that implements the IEnergyStorage interface. The block must contain enough energy
within itself and also be able to manage enough throughput: If the need requires more energy at once, the Xoonglin may
not be able to extract it. This can be tuned by balancing amount and frequency.

<details>
    <summary>Sample energy need file</summary>

```json
{
  "type": "cft:energy",
  "id": "basic_energy_need",
  "damage": 1.0,
  "damage_threshold": 0.2,
  "provided_happiness": 1.0,
  "satisfaction_threshold": 0.8,
  "frequency": 1.0,
  "energy_amount": 50
}
```
The only specific field for this need is the energy amount.
- `energy_amount`: The amount that must be consumed in one go to satisfy the need, in NeoForge Energy units.
</details>

#### Social Need

The Xoonglin needs companions of a specific social class nearby.

<details>
    <summary>Sample social need file</summary>

```json
{
  "type": "cft:social",
  "id": "cft:settlers_companions_need",
  "damage": 0.5,
  "damage_threshold": 0.2,
  "provided_happiness": 1.0,
  "satisfaction_threshold": 0.8,
  "frequency": 0.2,
  "hidden": false,
  "classes": ["cft:settler"],
  "min_count": 2,
  "max_count": 30,
  "radius": 50
}
```
- `classes`: A list of social class IDs. Xoonglins of these classes count as companions.
- `min_count`: Minimum number of companions needed.
- `max_count`: Maximum number of companions that count toward satisfaction.
- `radius`: How far to search for companions.
</details>

#### Pet Need

The Xoonglin needs some mobs of specific types to be around (or to be absent: this
need can also be used for limiting the presence of some mobs, e.g. hostile ones).

<details>
    <summary>Sample pet need file</summary>

```json
{
  "type": "cft:pet",
  "id": "cft:cat_companion_need",
  "damage": 0.5,
  "damage_threshold": 0.2,
  "provided_happiness": 3.0,
  "satisfaction_threshold": 0.8,
  "frequency": 1.0,
  "hidden": false,
  "entity_types": ["minecraft:cat"],
  "min_count": 1,
  "max_count": 5,
  "radius": 16
}
```
- `entity_types`: A list of entity type IDs that count as valid pets.
- `min_count`: Minimum number of matching pets needed.
- `max_count`: Maximum number of pets that count toward satisfaction.
- `radius`: How far to search for pets.
</details>

#### Lighting Need

The Xoonglin needs a certain light level around them.

<details>
    <summary>Sample lighting need file</summary>

```json
{
  "type": "cft:lighting",
  "id": "cft:well_lit_need",
  "damage": 0.3,
  "damage_threshold": 0.5,
  "provided_happiness": 2.0,
  "satisfaction_threshold": 0.75,
  "frequency": 0.5,
  "hidden": true,
  "min_light": 8,
  "radius": 3
}
```
- `min_light`: Minimum light level (0-15) required for the need to be satisfied.
- `radius`: _(Optional, default: 0)_ Sampling radius around the Xoonglin's position.
  If 0, only the block at the Xoonglin's position is checked.
</details>

#### Decoration Need

The Xoonglin needs specific decorative blocks placed near their home.

<details>
    <summary>Sample decoration need file</summary>

```json
{
  "id": "cft:flower_garden_need",
  "type": "cft:decoration",
  "damage": 0.2,
  "damage_threshold": 0.5,
  "provided_happiness": 5.0,
  "satisfaction_threshold": 0.75,
  "frequency": 1.0,
  "block_tag": "minecraft:flowers",
  "min_count": 6,
  "radius": 8,
  "min_spread": 0.3
}
```
- `block` or `block_tag`: The decorative block or tag of blocks to look for.
- `min_count`: Minimum number of matching blocks required.
- `radius`: Search radius around the home entrance.
- `min_spread`: _(Optional, default: 0.0)_ Minimum spatial spread of the blocks (0 to 1).
</details>

#### Ritual Need

The Xoonglin needs a ritual to take place nearby. Despite the name, this doesn't have to
be anything religious: any kind of ceremony, event or performance can be configured with
this need type: a communal meal, a market day, a concert, a festival... Rituals are
performed by Xoonglins with the **Officiant job** (see the Jobs section), and the need is
satisfied when a ritual with a matching `ritual_id` completes within the given radius.

<details>
    <summary>Sample ritual need file</summary>

```json
{
  "type": "cft:ritual",
  "id": "cft:communion_need",
  "damage": 0.0,
  "damage_threshold": 0.0,
  "provided_happiness": 5.0,
  "satisfaction_threshold": 0.9,
  "frequency": 1.0,
  "hidden": false,
  "ritual_id": "cft:communion",
  "radius": 32,
  "requires_presence": true
}
```

For the common fields, look at the goods need example. The specific fields for ritual needs are:

- `ritual_id`: Identifier of the ritual that satisfies this need. It must match the
  `ritual_id` of an officiant job for the ritual to ever be performed.
- `radius`: _(Optional, default: 16)_ How close (in blocks) the ritual must take place
  for this Xoonglin to benefit from it.
- `requires_presence`: _(Optional, default: false)_ If true, the Xoonglin must attend the
  ritual in person, from beginning to end, standing within the ritual radius. If false,
  it is enough that the ritual completes nearby, wherever the Xoonglin happens to be.
</details>

### Jobs

Jobs define what Xoonglins do during the day. They are assigned via the `jobs` field in
a social class definition. Each social class can list multiple jobs, and each Xoonglin
will randomly pick one from the list. A Xoonglin with a job will work a configurable
number of hours per Minecraft day, tracked through a daily tick quota.

Job progress can be viewed in the **Job tab** of the Xoonglin info screen (accessed via
the leader staff).

#### Home Artisan

The Xoonglin works at home and produces items periodically.

<details>
    <summary>Sample home artisan job file</summary>

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
</details>

#### Gatherer

The Xoonglin wanders near home, breaks matching blocks, and deposits the drops in the
home container.

<details>
    <summary>Sample gatherer job file</summary>

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
</details>

#### Guard

The Xoonglin patrols around their home and attacks hostile mobs that come nearby.

<details>
    <summary>Sample guard job file</summary>

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
</details>

#### Farmer

The Xoonglin works at a farm structure, planting seeds and harvesting crops when they
are ripe. The farmer needs a farm structure (open air platform) to work at, and will
plant seeds on farmland blocks within the farm, then harvest the crop when it reaches
the ripe state.

<details>
    <summary>Sample farmer job file</summary>

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
</details>

#### Hauler

The Xoonglin moves items between structures. Each errand defines an origin structure,
a destination structure and the items to transport. The hauler will take items from the
origin's container, carry them in its inventory and deposit them in the destination's
container. Carried items are visible in the **Items tab** of the Xoonglin info screen.

<details>
    <summary>Sample hauler job file</summary>

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
</details>

#### Builder

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

<details>
    <summary>Sample builder job file</summary>

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
</details>

#### Officiant

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

<details>
    <summary>Sample officiant job file</summary>

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
</details>



### Structures

Structures define building types that the mod can recognize and validate in the world.
Each structure type specifies which blocks are valid for its different parts, and how
the building should be shaped. Structures are detected by right clicking their **key block**
with the **Leader Staff**.

All structure types share these base fields:

- `type`: The structure type discriminator (e.g. `"cft:house"`, `"cft:enclosed_building"`,
  `"cft:open_air_platform"`, `"cft:monument"`, `"cft:multi_storey_building"`).
- `id`: Identifier of this structure type.
- `key_block`: _(Optional)_ A specific block that identifies the structure. Right clicking
  this block with the leader staff will trigger detection.
- `key_block_tag`: _(Optional)_ A block tag; any block in the tag can serve as the key block.
  One of `key_block` or `key_block_tag` should be provided.
- `max_users`: _(Optional, default: 1)_ Maximum number of Xoonglins that can use this structure
  at the same time. Set to 0 for structures with no user limit (e.g. monuments).
- `requires_container`: _(Optional, default: false)_ Whether the structure must contain a
  container block (e.g. a chest). Houses that need to store supplies should set this to `true`.
- `priority`: _(Optional, default: 0)_ Priority for structure selection. Higher values are
  preferred when multiple structures of the same category are available.

Block rules are specified using `ValidBlock` objects with these fields:

- `block`: A reference to a specific block (e.g. `"minecraft:stone_bricks"`).
- `tagBlock`: **Alternatively**, a block tag (e.g. `"minecraft:planks"`) so all blocks
  belonging to the tag are accepted.
- `minQuantity`: At least this many blocks of this type must be present.
- `maxQuantity`: No more than this many blocks of this type can be present.
- `minPercentage`: This part of the structure must have at least this percentage of
  blocks of this type. Always in [0,1].
- `maxPercentage`: This part of the structure can't have more than this percentage of
  blocks of this type. Always in [0,1].

#### House

Houses are enclosed buildings that serve as homes for Xoonglins. They are a specialized
form of enclosed buildings with doors as key blocks. A house consists of four parts:
floor, walls, interior and roof.

The floor can take any shape. Walls are built over the most exterior part of the floor
upwards and must all be of the same height. The roof must be built resting on the walls
and covering the full surface of the house.

Houses that need to store supplies for their Xoonglin should declare `"requires_container": true`
and include the container block (e.g. a chest) in the `interiorBlocks` list.

<details>
    <summary>Sample house structure file</summary>

```json
{
  "type": "cft:house",
  "id": "cft:settler_house",
  "key_block_tag": "minecraft:doors",
  "max_users": 1,
  "requires_container": true,
  "priority": 0,
  "floorBlocks": [
    {
      "tagBlock": "minecraft:planks",
      "minQuantity": 9,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "wallBlocks": [
    {
      "tagBlock": "minecraft:logs",
      "minQuantity": 14,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "tagBlock": "minecraft:planks",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "tagBlock": "minecraft:doors",
      "minQuantity": 1,
      "maxQuantity": 2,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "interiorBlocks": [
    {
      "block": "minecraft:air",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "block": "minecraft:chest",
      "minQuantity": 1,
      "maxQuantity": 1,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "roofBlocks": [
    {
      "tagBlock": "minecraft:wooden_stairs",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ]
}
```

- `floorBlocks`: A list of valid block rules for the floor.
- `wallBlocks`: A list of valid block rules for the walls. Doors should be included here.
- `interiorBlocks`: A list of valid block rules for the interior. Air should always be present.
  Include the container block (e.g. chest) if the house requires one.
- `roofBlocks`: A list of valid block rules for the roof.
</details>

#### Enclosed Building

Enclosed buildings follow the same structure as houses (floor, walls, interior, roof) but
are not homes. They are used as workplaces or other facilities (e.g. a smithy).

<details>
    <summary>Sample enclosed building structure file</summary>

```json
{
  "type": "cft:enclosed_building",
  "id": "cft:smithy",
  "key_block": "minecraft:anvil",
  "max_users": 2,
  "requires_container": true,
  "priority": 0,
  "floorBlocks": [
    {
      "tagBlock": "minecraft:stone_bricks",
      "minQuantity": 4,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "wallBlocks": [
    {
      "tagBlock": "minecraft:stone_bricks",
      "minQuantity": 4,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "block": "minecraft:oak_door",
      "minQuantity": 1,
      "maxQuantity": 2,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "interiorBlocks": [
    {
      "block": "minecraft:air",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "block": "minecraft:chest",
      "minQuantity": 1,
      "maxQuantity": 1,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "block": "minecraft:anvil",
      "minQuantity": 1,
      "maxQuantity": 1,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "roofBlocks": [
    {
      "block": "minecraft:oak_planks",
      "minQuantity": 4,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ]
}
```

The fields are identical to house structures. The key difference is that enclosed buildings
use a specific `key_block` (like an anvil) instead of doors, and they are not used as homes.
</details>

#### Open Air Platform

Open air platforms are flat structures surrounded by a border, such as farms or pens.
They don't have a roof or enclosed walls — instead they have a border (like fences), a
ground perimeter, and a surface.

<details>
    <summary>Sample open air platform structure file</summary>

```json
{
  "type": "cft:open_air_platform",
  "id": "cft:farm",
  "key_block_tag": "minecraft:fence_gates",
  "max_users": 1,
  "requires_container": false,
  "priority": 0,
  "wall_height": 1,
  "borderBlocks": [
    {
      "tagBlock": "minecraft:fences",
      "minQuantity": 4,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "tagBlock": "minecraft:fence_gates",
      "minQuantity": 1,
      "maxQuantity": 1,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "block": "minecraft:chest",
      "minQuantity": 1,
      "maxQuantity": 1,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "groundPerimeterBlocks": [
    {
      "tagBlock": "minecraft:dirt",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    },
    {
      "block": "minecraft:grass_block",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "surfaceBlocks": [
    {
      "block": "minecraft:farmland",
      "minQuantity": 4,
      "maxQuantity": 500,
      "minPercentage": 0.5,
      "maxPercentage": 1.0
    },
    {
      "block": "minecraft:water",
      "minQuantity": 1,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 0.25
    },
    {
      "tagBlock": "minecraft:dirt",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 0.25
    }
  ]
}
```

- `wall_height`: _(Optional, default: 1)_ Height of the border/wall around the platform.
- `borderBlocks`: A list of valid block rules for the border surrounding the platform
  (e.g. fences, fence gates).
- `groundPerimeterBlocks`: A list of valid block rules for the ground below the border.
- `surfaceBlocks`: A list of valid block rules for the interior surface of the platform
  (e.g. farmland, water).
</details>

#### Monument

Monuments are vertical structures validated layer by layer, such as obelisks or towers.
Each layer is a horizontal slice of blocks, and rules can specify which blocks are valid
at different height ranges.

<details>
    <summary>Sample monument structure file</summary>

```json
{
  "type": "cft:monument",
  "id": "cft:obelisk",
  "key_block": "minecraft:chiseled_quartz_block",
  "max_users": 0,
  "requires_container": false,
  "priority": 0,
  "min_height": 5,
  "max_height": 15,
  "layerRules": [
    {
      "from": 0,
      "to": 0,
      "blocks": [
        {
          "block": "minecraft:chiseled_quartz_block",
          "minQuantity": 1,
          "maxQuantity": 1,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:quartz_block",
          "minQuantity": 0,
          "maxQuantity": 8,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ]
    },
    {
      "from": 1,
      "to": 14,
      "blocks": [
        {
          "block": "minecraft:quartz_block",
          "minQuantity": 1,
          "maxQuantity": 9,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ]
    }
  ],
  "identicalLayerGroups": [
    {
      "from": 1,
      "to": 14
    }
  ]
}
```

- `min_height`: Minimum height of the monument in blocks.
- `max_height`: Maximum height of the monument in blocks.
- `layerRules`: A list of rules, each applying to a range of layers (from bottom to top).
  Layer 0 is the base.
  - `from`: First layer index this rule applies to (inclusive).
  - `to`: Last layer index this rule applies to (inclusive).
  - `blocks`: A list of valid block rules for layers in this range.
- `identicalLayerGroups`: _(Optional)_ Groups of layers that must be identical to each other.
  - `from`: First layer index of the group (inclusive).
  - `to`: Last layer index of the group (inclusive).
</details>

#### Multi-Storey Building

Multi-storey buildings are made of stacked enclosed-building storeys. Each storey works
like an enclosed building — it has its own floor, walls, interior and roof (ceiling) —
and on top of it sits the next storey. Storey rules define the valid blocks for each
storey or range of storeys, similar to how monument layer rules work.

Two consecutive storeys can be connected in either of two ways, tried in this order:

- **Shared layer**: the ceiling of the lower storey is at the same time the floor of the
  upper one. That layer must satisfy both the lower storey's `roofBlocks` rules and the
  upper storey's `floorBlocks` rules.
- **Separate layers**: the floor of the upper storey sits exactly one block above the
  ceiling of the lower one, forming a two-block-thick separation.

All storeys share the same footprint, and every ceiling that supports another storey
must be flat (the top roof can take any shape as long as its walls vary in height).
Detection starts from the key block of the ground storey and stacks storeys upward until
one no longer fits; the building is valid if at least `min_storeys` are found.

Xoonglins need a way to move between storeys, and holes in a ceiling are controlled
through the block rules: list the connection blocks (ladders, or air above stairs) in the
lower storey's `roofBlocks` — and, when the layer is shared, in the upper storey's
`floorBlocks` too. Using `minQuantity` and `maxQuantity` you can require a connection to
exist and limit how large the opening can be. The mod ships two examples:
`two_storey_house.json` (ladder connection) and `two_storey_stairs_house.json` (stairs
below an air opening).

<details>
    <summary>Sample multi-storey building structure file</summary>

```json
{
  "type": "cft:multi_storey_building",
  "id": "cft:two_storey_house",
  "key_block": "minecraft:oak_door",
  "max_users": 2,
  "requires_container": true,
  "priority": 10,
  "min_storeys": 2,
  "max_storeys": 2,
  "storeyRules": [
    {
      "from": 1,
      "to": 1,
      "floorBlocks": [
        {
          "tagBlock": "minecraft:planks",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "wallBlocks": [
        {
          "tagBlock": "minecraft:logs",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:oak_door",
          "minQuantity": 1,
          "maxQuantity": 2,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "interiorBlocks": [
        {
          "block": "minecraft:air",
          "minQuantity": 0,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:ladder",
          "minQuantity": 1,
          "maxQuantity": 10,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:chest",
          "minQuantity": 1,
          "maxQuantity": 2,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "roofBlocks": [
        {
          "tagBlock": "minecraft:planks",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:ladder",
          "minQuantity": 1,
          "maxQuantity": 1,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ]
    },
    {
      "from": 2,
      "to": 2,
      "floorBlocks": [
        {
          "tagBlock": "minecraft:planks",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:ladder",
          "minQuantity": 0,
          "maxQuantity": 1,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "wallBlocks": [
        {
          "tagBlock": "minecraft:logs",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "interiorBlocks": [
        {
          "block": "minecraft:air",
          "minQuantity": 0,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:ladder",
          "minQuantity": 0,
          "maxQuantity": 10,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "roofBlocks": [
        {
          "tagBlock": "minecraft:planks",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ]
    }
  ]
}
```

- `min_storeys`: Minimum number of storeys the building must have.
- `max_storeys`: Maximum number of storeys. Detection never looks beyond this.
- `storeyRules`: A list of rules, each applying to a range of storeys (from bottom to
  top). Storey 1 is the ground storey. Every storey from 1 to `max_storeys` must be
  covered by exactly one rule.
  - `from`: First storey this rule applies to (inclusive).
  - `to`: Last storey this rule applies to (inclusive).
  - `floorBlocks`: A list of valid block rules for the storey's floor.
  - `wallBlocks`: A list of valid block rules for the storey's walls. Doors should be
    included in the ground storey's rules.
  - `interiorBlocks`: A list of valid block rules for the storey's interior.
  - `roofBlocks`: A list of valid block rules for the storey's roof (ceiling). Include
    connection blocks (ladders) or air openings here for storeys that must be reachable
    from below.
</details>
