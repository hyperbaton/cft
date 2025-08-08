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
crouching) or know current state of a xungui.
- The first Xoonglins will spawn spontanously, but from then on, they will mate to increase
their population. However, they will always respect a given social structure (a relation
on the amount of Xoonglins of each class).
- You can compete with other players for getting the biggest and happiest Xoonglin
population. The commands `\happinessLadder`, `\populationLadder` and `\socialstructure` give
rankings and information on your Xoonglins.
- The social classes and needs are fully configurable and customizable via datapacks,
so it's possible to build a tailored experience for any modpack.

## Datapacks

It is possible to configure the social classes and needs of Xoonglins via datapacks, and
any aspect of them can be configured.

Some sample classes and needs come packaged with the mod, but they are only intended
as examples for the possibilities of the mod. It is strongly advised to create a
datapack with specific classes for a given modpack.

The datapack documentation is presented below.

### Social Class

<details>
    <summary>Sample social class file</summary>

```json
{
  "id": "cft:patrician",
  "maxHappiness": 4000.0,
  "matingHappinessThreshold": 800.0,
  "spontaneouslySpawnPopulation": 0,
  "needs": [
    "cft:gold_nugget_need",
    "cft:book_need",
    "cft:pork_need",
    "cft:patrician_home"
  ],
  "upgrades": [
    {
      "nextClass": "cft:noble",
      "requiredHappiness": 1000,
      "requiredNeeds": [
        {
          "need": "cft:gold_nugget_need",
          "satisfactionThreshold": 0.85
        }
      ],
      "socialStructureRequirements": [
        {
          "socialClass": "cft:patrician",
          "percentage": 0.06
        }
      ]
    }
  ],
  "downgrades": [
    {
      "nextClass": "cft:citizen",
      "requiredHappiness": 60,
      "requiredNeeds": [
        {
          "need": "cft:gold_nugget_need",
          "satisfactionThreshold": 0.40
        }
      ],
      "socialStructureRequirements": []
    }
  ]
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
- `upgrades`: A list of ways a Xoonglin can become a higher class.
    - `nextClass`: Reference to next class.
    - `requiredHappiness`: Minimum happiness level to consider upgrading.
    - `requiredNeeds`: These needs have to be satisfied at the given value for the upgrade
      to be possible.
    - `socialStructureRequirements`: A list of such requirements. Each social class mentioned
      must represent a percentage lower or equal to this one. Always in the range [0,1].
- `downgrade`: A list of ways a Xoonglin can become a lower class.
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
try to get the goods from the chest whithin their home.

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
  "quantity": 2
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
- `hidden`: An optional boolean indicating if this need should be hidden from interfaces. Its value is `false`
by default.
- `quantity`: How many items of the specified class are needed to satisfy the need.
</details>

#### Home Need

A home consists of four parts: floor, walls, interior and roof. In a home need, it is
specified which blocks can be used for each of these parts, and in which quantity.
Breaking any of these rules will make a house invalid.

Once built, right clicking with the **Leader Staff** on the door of a house will check if it's valid.
A message will appear in chat informing if it is or not.

All houses must have one chest and one wooden door. Else, they are invalid.

The floor can take any shape. Then, walls are built over the most exterior part of the
floor upwards. The walls must be all of the same height. Then, the roof must be built
resting on the walls and covering the full surface of the house.

<details>
    <summary>Sample home need file</summary>

```json
{
  "id": "cft:soldier_home",
  "type": "cft:home",
  "damage": 0.0,
  "damage_threshold": 0.0,
  "provided_happiness": 20,
  "satisfaction_threshold": 0.9,
  "frequency": 1.0,
  "floorBlocks": [
    {
      "tagBlock": "minecraft:planks",
      "minQuantity": 16,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ],
  "wallBlocks": [
    {
      "block": "minecraft:iron_bars",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.05,
      "maxPercentage": 0.20
    },
    {
      "block": "minecraft:stone_bricks",
      "minQuantity": 0,
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
    }
  ],
  "roofBlocks": [
    {
      "block": "minecraft:stone_brick_slab",
      "minQuantity": 0,
      "maxQuantity": 500,
      "minPercentage": 0.0,
      "maxPercentage": 1.0
    }
  ]
}

```


For the common fields, look at the good need example. The rest is a specification of
the blocks a home can be built with:

- `floorBlocks`: A list of possible blocks for the floor. It works in the same way for
the rest of home parts (wall, interior and roof). It is a list of elements whose
fields are:
  - `block`: A reference to a block that can be used for this part of the house.
  - `tagBlock`: **Alternatively**, a tag can be provided so all blocks that belong to the 
  tag will be taken into consideration.
  - `minQuantity`: At least, this amount of blocks of this type must be present.
  - `minQuantity`: No more than this amount of blocks of this type must be present.
  - `minPercentage`: This part of the home must have at least this percentage of blocks
  of this type. Always in [0,1]
  - `maxPercentage`: This part of the home can't have more than this percentage of blocks
  of this type. Always in [0,1]

For the interior blocks, air should always be present.
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

It's similar to the Goods Need, but in this case the goods are fluids that are taken from a container withing the home
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
Apart from the common fields, this need includes a fluid stack object. Yes, it's in PascalCase because it uses Forge
parsing method for FluidStack.
- `fluid_stack`: A FluidStack object that contains the reference of the fluid and the amount in milibuckets. 
</details>

#### Energy Need

Works very similar to the Fluid Needs, but in this case the product consumed is just Forge Energy. It can be taken from
any block within the Xoonglin's home that implements the IEnergyStorage interface. The block must contain enough energy
within itself and also be able to manage enough throughput: If the need requires more energy at once, the Xoonglin may
not be able to extract it. This can be tuned by balancing amount and frequency.

<details>
    <summary>Sample fluid need file</summary>

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
- `energy_amount`: The amount that must be consumed in one go to satisfy the need, in Forge Energy units.
</details>