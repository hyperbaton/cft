# Structures

Structures define building types that the mod can recognize and validate in the world.
Each structure type specifies which blocks are valid for its different parts, and how
the building should be shaped. Structures are detected by right clicking their **key block**
with the **Leader Staff**.

### Keeping structures up to date

A registered structure is detected again right before it's used, and unregistered if it no
longer passes:

- when a Xoonglin claims it as its home or for its job or needs;
- when the [home need](needs.md#home-need) or a structure need with `requires_usage` is checked,
  at the need's `frequency`;
- when a Xoonglin arrives at it to satisfy a [visit need](needs.md);
- when a Xoonglin starts working at it (each day, or after its work was paused);
- when its leader right clicks its key block with the staff again. This works as a regular
  detection: the structure is kept if it's still of the same type (taking any blocks added,
  like a new chest), replaced if it now matches another type, and unregistered if it fails.

Breaking the key block unregisters the structure right away. Xoonglins using an unregistered
structure give it up at the next of those moments, and look for another one. The structure a
job takes apart as it works (a miner's quarry, for example) isn't detected again while it's in use;
the job retires it itself.

All structure types share these base fields:

- `type`: The structure type discriminator (e.g. `"cft:house"`, `"cft:enclosed_building"`,
  `"cft:open_air_platform"`, `"cft:pasture"`, `"cft:monument"`, `"cft:multi_storey_building"`,
  `"cft:compound"`).
- `key_block`: The block that identifies the structure: right clicking it with the leader staff
  triggers detection. A block, a block tag or a list of them (see [Game content](index.md#game-content)).
- `max_users`: _(Optional, default: 1)_ Maximum number of Xoonglins that can use this structure
  at the same time. Set to 0 for structures with no user limit (e.g. monuments).
- `requires_container`: _(Optional, default: false)_ Whether the structure must contain a
  container block (e.g. a chest). Houses that need to store supplies should set this to `true`.
- `priority`: _(Optional, default: 0)_ Priority for structure selection. Higher values are
  preferred when multiple structures of the same category are available.

Block rules are specified using `ValidBlock` objects with these fields:

- `block`: The blocks the rule is about: a block (e.g. `"minecraft:stone_bricks"`), a block
  tag (e.g. `"#minecraft:planks"`) or a list of them (see [Game content](index.md#game-content)).
- `minQuantity`: At least this many blocks of this type must be present.
- `maxQuantity`: No more than this many blocks of this type can be present.
- `minPercentage`: This part of the structure must have at least this percentage of
  blocks of this type. Always in [0,1].
- `maxPercentage`: This part of the structure can't have more than this percentage of
  blocks of this type. Always in [0,1].

### Lighting

Buildings with an inside (houses, enclosed buildings and multi-storey buildings) can require it
to be lit, with an optional `lighting` object:

```json
{"lighting": { "min_light": 8, "min_percentage": 0.8 }}
```

- `min_light`: The light (0 to 15) a spot needs to count as lit.
- `min_percentage`: _(Optional, default: 1.0)_ The share of the floor that must be lit, in
  [0,1].

The light is measured on the spots where a Xoonglin can stand: right above the floor, on every
storey. Only light from blocks counts (torches, lanterns, glowstone...), not daylight, so a
building is lit or not at any time of day. With `"min_light": 1` and the default percentage, the
building can have no dark corner, which is also where monsters can't spawn.

## House

Houses are enclosed buildings that serve as homes for Xoonglins. They are a specialized
form of enclosed buildings with doors as key blocks. A house consists of four parts:
floor, walls, interior and roof.

The floor can take any shape. Walls are built over the most exterior part of the floor
upwards and must all be of the same height. The roof must be built resting on the walls
and covering the full surface of the house.

Houses that need to store supplies for their Xoonglin should declare `"requires_container": true`
and include the container block (e.g. a chest) in the `interiorBlocks` list.

??? example "Sample house structure file"

    ```json
    {
      "type": "cft:house",
      "key_block": "#minecraft:doors",
      "max_users": 1,
      "requires_container": true,
      "priority": 0,
      "floorBlocks": [
        {
          "block": "#minecraft:planks",
          "minQuantity": 9,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "wallBlocks": [
        {
          "block": "#minecraft:logs",
          "minQuantity": 14,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "#minecraft:planks",
          "minQuantity": 0,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "#minecraft:doors",
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
          "block": "#minecraft:wooden_stairs",
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
    - `lighting`: _(Optional)_ How well lit the inside must be (see [Lighting](#lighting)).

## Enclosed Building

Enclosed buildings follow the same structure as houses (floor, walls, interior, roof) but
are not homes. They are used as workplaces or other facilities (e.g. a smithy).

??? example "Sample enclosed building structure file"

    ```json
    {
      "type": "cft:enclosed_building",
      "key_block": "minecraft:anvil",
      "max_users": 2,
      "requires_container": true,
      "priority": 0,
      "floorBlocks": [
        {
          "block": "#minecraft:stone_bricks",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "wallBlocks": [
        {
          "block": "#minecraft:stone_bricks",
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

## Open Air Platform

Open air platforms are flat structures surrounded by a border, such as farms or pens.
They don't have a roof or enclosed walls — instead they have a border (like fences), a
ground perimeter, and a surface.

??? example "Sample open air platform structure file"

    ```json
    {
      "type": "cft:open_air_platform",
      "key_block": "#minecraft:fence_gates",
      "max_users": 1,
      "requires_container": false,
      "priority": 0,
      "wall_height": 1,
      "borderBlocks": [
        {
          "block": "#minecraft:fences",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "#minecraft:fence_gates",
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
          "block": "#minecraft:dirt",
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
          "block": "#minecraft:dirt",
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

## Pasture

A pasture is an open air platform (same border/ground perimeter/surface shape) that
additionally requires a minimum — and optionally maximum — number of specific animals to
be physically present inside its footprint. The animals are counted again every time the
structure is detected again (see [Keeping structures up to date](#keeping-structures-up-to-date)),
so a pen that's since wandered empty is unregistered rather than staying claimed on stale data.

??? example "Sample pasture structure file"

    ```json
    {
      "type": "cft:pasture",
      "key_block": "minecraft:hay_block",
      "max_users": 1,
      "requires_container": false,
      "priority": 0,
      "wall_height": 1,
      "borderBlocks": [
        {
          "block": "#minecraft:fences",
          "minQuantity": 4,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "groundPerimeterBlocks": [
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
          "block": "minecraft:grass_block",
          "minQuantity": 0,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "eligible_mobs": [
        "minecraft:cow",
        "minecraft:sheep",
        "minecraft:pig"
      ],
      "min_mob_count": 3
    }
    ```

    - `wall_height`, `borderBlocks`, `groundPerimeterBlocks`, `surfaceBlocks`: Same as open air
      platform.
    - `eligible_mobs`: The animals that count toward the pasture's occupancy: entity types
      (e.g. `"minecraft:cow"`), entity type tags (e.g. `"#minecraft:skeletons"`) or a list of
      them (see [Game content](index.md#game-content)).
    - `min_mob_count`: _(Optional, default: 1)_ Minimum number of eligible animals that must
      be inside the footprint.
    - `max_mob_count`: _(Optional, default: unlimited)_ Maximum number of eligible animals
      allowed inside the footprint.

## Monument

Monuments are vertical structures validated layer by layer, such as obelisks or towers.
Each layer is a horizontal slice of blocks, and rules can specify which blocks are valid
at different height ranges.

??? example "Sample monument structure file"

    ```json
    {
      "type": "cft:monument",
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

## Multi-Storey Building

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

??? example "Sample multi-storey building structure file"

    ```json
    {
      "type": "cft:multi_storey_building",
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
              "block": "#minecraft:planks",
              "minQuantity": 4,
              "maxQuantity": 500,
              "minPercentage": 0.0,
              "maxPercentage": 1.0
            }
          ],
          "wallBlocks": [
            {
              "block": "#minecraft:logs",
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
              "block": "#minecraft:planks",
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
              "block": "#minecraft:planks",
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
              "block": "#minecraft:logs",
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
              "block": "#minecraft:planks",
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
    - `lighting`: _(Optional)_ How well lit the inside must be, all storeys together (see
      [Lighting](#lighting)).

## Compound

Compounds are structures composed of other structures: an open surface (a plaza, a
village square, a courtyard, a market...) that is only valid if enough already detected
structures of the required types stand close to it. Instead of walls or fences, the
surrounding buildings act as the compound's "border".

The surface is detected by flood fill starting below the key block, so the paving
material must be different from the surrounding ground for the surface to have a
defined shape. The key block can stand directly on the paving or on top of a small
decorative pillar (up to 5 blocks tall, e.g. a bell on a stone column); the pillar
blocks are ignored by validation. The referenced structures must be detected (with the
leader staff) **before** the compound, and must belong to the same leader.

A compound cannot share surface blocks with any already detected compound, so placing
a second key block on an already detected square will not create a second compound.

??? example "Sample compound structure file"

    ```json
    {
      "type": "cft:compound",
      "key_block": "minecraft:bell",
      "max_users": 0,
      "requires_container": false,
      "priority": 0,
      "surfaceBlocks": [
        {
          "block": "#minecraft:stone_bricks",
          "minQuantity": 25,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        },
        {
          "block": "minecraft:polished_andesite",
          "minQuantity": 0,
          "maxQuantity": 500,
          "minPercentage": 0.0,
          "maxPercentage": 1.0
        }
      ],
      "requiredStructures": [
        {
          "structure_type": "cft:settler_house",
          "min": 3,
          "max_distance": 16
        }
      ],
      "requires_sky_access": true
    }
    ```

    - `surfaceBlocks`: A list of valid block rules for the compound's surface (the paving).
    - `requiredStructures`: A list of requirements on nearby detected structures. All of them
      must be met for the compound to be valid.
        - `structure_type`: A reference to the structure type ID that must exist nearby.
        - `min`: _(Optional, default: 1)_ Minimum number of structures of this type.
        - `max`: _(Optional, default: unlimited)_ Maximum number of structures of this type.
        - `max_distance`: _(Optional, default: 16)_ Maximum distance (in blocks) from the
        structure's key block to the nearest surface block of the compound.
    - `requires_sky_access`: _(Optional, default: true)_ Whether every surface block must be
      open to the sky.
