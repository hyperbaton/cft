# Datapacks

It is possible to configure the social classes, needs, structures and jobs of Xoonglins
via datapacks, and any aspect of them can be configured.

Some sample classes and needs come packaged with the mod, but they are only intended
as examples for the possibilities of the mod: the [Sample datapack](sample-datapack.md) page
tells what each one shows. It is strongly advised to create a datapack with specific classes for
a given modpack.

These pages document each kind of datapack file:

- [Social classes](social-classes.md): the classes of Xoonglins, with their needs and jobs.
- [Schedules](schedules.md): when Xoonglins work, have free time and rest.
- [Needs](needs.md): what Xoonglins need to be happy.
- [Jobs](jobs.md): the work Xoonglins do.
- [Structures](structures.md): the buildings the mod recognizes, like homes and workshops.
- [Sample datapack](sample-datapack.md): the classes that come with the mod, and where to find an
  example of each feature.

## Files and ids

Each kind of file goes in its own folder, and its id is given by where it is, as with vanilla
recipes or loot tables: `data/<namespace>/cft/<folder>/<name>.json` has the id
`<namespace>:<name>`. Files refer to each other by these ids. For example, a social class with
`cft:bread_need` in its `needs` uses the need in `data/cft/cft/need/bread_need.json`.

| Kind | Folder |
|---|---|
| Social classes | `data/<namespace>/cft/social_class/` |
| Needs | `data/<namespace>/cft/need/` |
| Jobs | `data/<namespace>/cft/job/` |
| Structures | `data/<namespace>/cft/structure/` |

Subfolders are part of the id: `data/cft/cft/need/food/bread.json` is `cft:food/bread`.

### Names

Social classes, needs, jobs and structures are shown by name, translated like vanilla content.
Their lang key is the folder, the namespace and the path of the id, joined by dots; slashes in
the path become dots too. Add the names to your resource pack's lang file:

| Kind | Lang key | Example |
|---|---|---|
| Social class | `social_class.<namespace>.<path>` | `social_class.cft.citizen` |
| Need | `need.<namespace>.<path>` | `need.cft.music_need` |
| Need description, in its tooltip | `need.<namespace>.<path>.tooltip` | `need.cft.music_need.tooltip` |
| Job | `job.<namespace>.<path>` | `job.cft.baker_job` |
| Structure | `structure.<namespace>.<path>` | `structure.cft.smithy` |

So `cft:food/bread` is named by `need.cft.food.bread`.

### Textures

Each social class can have its own Xoonglin texture, in a resource pack of the same namespace as
the class: `assets/<namespace>/textures/entity/<path>.png`. For example, `cft:citizen` uses
`assets/cft/textures/entity/citizen.png`. A class without one uses the default Xoonglin texture.

## Game content

Fields that refer to game content, like blocks, entity types, biomes or sounds, take it the way
vanilla data does:

- an id: `"minecraft:furnace"`;
- a tag, with a leading `#`: `"#minecraft:logs"`, for anything in the tag;
- or a list mixing both: `["minecraft:furnace", "#minecraft:campfires"]`.

An id that doesn't exist stops the datapack from loading, with an error that names the file, so
typos don't go unnoticed. Biomes are the exception: they come from datapacks themselves, so a
wrong biome id just never matches. A tag that doesn't exist matches nothing.
