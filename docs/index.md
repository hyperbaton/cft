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
- Each Xoonglin belongs to a **social class**. Each of these has a different set of social
needs. If their needs are satisfied and their happiness increases, they can upgrade to
higher classes; however, if they get unhappy, they can demote to a lower class.
- The **leader staff** can be used to designate a home (by right-clicking on doors while
crouching), detect structures (by right-clicking on a structure's key block, again to check
a registered one) or know the current state of a Xoonglin.
- The first Xoonglins will spawn spontaneously, but from then on, they will mate to increase
their population. However, they will always respect a given social structure (a relation
 to the number of Xoonglins of each class).
- You can compete with other players for getting the biggest and happiest Xoonglin
population. The commands `\happinessLadder`, `\populationLadder` and `\socialstructure` give
rankings and information on your Xoonglins.
- The social classes, needs, structures and jobs are fully configurable and customizable via
datapacks, so it's possible to build a tailored experience for any modpack.
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
- **Social Class Browser**: Press `V` to open an interactive screen showing the full social
  class hierarchy as a visual graph, with clickable nodes to view each class's needs,
  stats, and upgrade/downgrade conditions.
- **Jade integration**: If [Jade](https://modrinth.com/mod/jade) is installed, looking at a
  Xoonglin shows its class, job and happiness; its leader also sees its most pressing need.

## Documentation

- [Datapacks](datapacks/index.md): how to configure social classes, schedules, needs, jobs and
  structures.
- [Addons](addons.md): how other mods can add new kinds of needs, jobs and structures.
