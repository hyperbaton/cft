# Datapacks

It is possible to configure the social classes, needs, structures and jobs of Xoonglins
via datapacks, and any aspect of them can be configured.

Some sample classes and needs come packaged with the mod, but they are only intended
as examples for the possibilities of the mod. It is strongly advised to create a
datapack with specific classes for a given modpack.

These pages document each kind of datapack file:

- [Social classes](social-classes.md): the classes of Xoonglins, with their needs and jobs.
- [Schedules](schedules.md): when Xoonglins work, have free time and rest.
- [Needs](needs.md): what Xoonglins need to be happy.
- [Jobs](jobs.md): the work Xoonglins do.
- [Structures](structures.md): the buildings the mod recognizes, like homes and workshops.

## Files and ids

Each kind of file goes in its own folder, and its id is given by where it is, as with vanilla
recipes or loot tables: `data/<namespace>/cft/<folder>/<name>.json` has the id
`<namespace>:<name>`. Files refer to each other by these ids. For example, a social class with
`cft:bread_need` in its `needs` uses the need in `data/cft/cft/need/bread_need.json`.

| Kind | Folder |
|---|---|
| Social classes | `data/<namespace>/cft/socialclass/` |
| Needs | `data/<namespace>/cft/need/` |
| Jobs | `data/<namespace>/cft/job/` |
| Structures | `data/<namespace>/cft/structure/` |

Subfolders are part of the id: `data/cft/cft/need/food/bread.json` is `cft:food/bread`.
