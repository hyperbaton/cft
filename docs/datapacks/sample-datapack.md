# Sample datapack

Care For Them comes with a datapack of its own: seven social classes with their needs, jobs and
structures. It's meant as an example to learn from and copy, more than as a balanced game; a
modpack is better off with classes of its own. Every kind of need, job and structure the mod has is
used somewhere in it, and so are most of the optional fields.

Its files are in the mod's jar, and in the
[repository](https://github.com/hyperbaton/cft/tree/1.21.x/src/main/resources/data/cft/cft), under
`data/cft/cft/`.

## The classes

- **Settler** and **Mountain dweller** upgrade to **Citizen**.
- **Citizen** upgrades to **Artisan**, **Soldier** or **Patrician**.
- **Patrician** upgrades to **Noble**.

Settlers and mountain dwellers are the two starting classes: they're the only ones that spawn on
their own. The rest are reached through upgrades, and each one can fall back through downgrades.
In game, press `V` to see this graph with the requirements of each step.

### Settler

The first settlers of a place: they need a roof, water and simple food, and they gather what's
around them.

- **Needs**: a home (`settler_house`), water, potatoes, bread, wood logs, other settlers nearby,
  sleep, and conversations.
- **Jobs**: hauler (wheat from the farm to the settler houses), builder (builds settler houses),
  flower gatherer and lumberjack (`lumber_camp`).
- **Worth a look**:
    - `water_need`: a fluid need (from a cauldron or any tank) with `active_when`: settlers drink
      rainwater, so it doesn't apply while it rains (`cft:not` and `cft:weather`).
    - `sleep_need`, with beds allowed in the `settler_house`.
    - `conversation_need`: a bonus socialize need.
    - `gather_flowers_job`: `available_to_babies`, so young settlers help too.
    - `settler_builder_job`: a builder that raises new settler houses by copying one already
      built, wherever the player places a door.
    - `name_samples`, as every class: each one has names of its own style.

### Mountain dweller

The other starting class, for settlements high in the mountains.

- **Needs**: a home (`mountain_dweller_house`), potatoes, wood logs, mountain biomes, high
  altitude, and warmth.
- **Jobs**: quarry miner (`quarry`) and smelter (`smeltery`).
- **Worth a look**:
    - `warmth_need`: coal or charcoal, only at night or in cold biomes (`cft:any_of`, `cft:time`
      and `cft:biome`).
    - `mountain_biomes_need` and `high_altitude_need`: needs met just by where the Xoonglin lives.
    - `quarry_miner_job`: digs out its quarry, which isn't detected again while it's mined.

### Citizen

The working town. Its upgrades lead to the three higher classes, each one through a different
hidden need.

- **Needs**: a home (`citizen_house`), bread, coal, communion, a visit to the tavern, music, and
  the hidden terracotta, book, iron sword and fishing rod needs.
- **Jobs**: baker and cook (`bakery`), fisher, rancher (`pasture`), farmer (`farm`), tavern
  keeper (`tavern`) and trader.
- **Worth a look**:
    - `citizen_terracotta_need`, `citizen_book_need` and `citizen_iron_sword_need`: `hidden`
      needs that decide which way a citizen upgrades (artisan, patrician or soldier).
    - `communion_need`: a ritual need, held by a priest.
    - `tavern_visit_need`: a visit need that `consumes` honey bottles, takes them from the
      tavern's chests (`use_supplies`) and only counts while a tavern keeper is working there
      (`requires_running`).
    - `music_need`: a bonus hearing need, with its own `icon`.
    - `tavern_keeper_job` and `trader_job`: traders with a `schedule` of their own.
    - `baker_job` and `cook_job`: crafting and smoking in the same bakery.

### Artisan

Craftsmen who make paper and copy books.

- **Needs**: a home (`artisan_house`), clay, terracotta, chicken, and reading.
- **Jobs**: paper maker (works at home) and scribe (`scriptorium`).
- **Worth a look**: `reading_need`, met with the books that patrician writers write and scribes
  copy.

### Soldier

The settlement's defenders and healers.

- **Needs**: a home (`soldier_house`), flint, an iron sword, chicken, and access to a smithy.
- **Jobs**: guard, healer and enchanter (`enchanting_room`).
- **Worth a look**:
    - The class `schedule`: soldiers start work at 5, before everyone else.
    - `max_health`: soldiers are tougher.
    - `smithy_access_need`: a structure need that requires using the smithy (`requires_usage`).
    - `guard_job`: only works while the iron sword need is met (`required_needs`).

### Patrician

The old families, who write and lead the rites.

- **Needs**: a home (`patrician_house`), gold nuggets, books, pork, a cat, and light.
- **Jobs**: writer and priest (`temple`).
- **Worth a look**:
    - `cat_companion_need`: a pet need.
    - `high_lighting_need`: a lighting need around the Xoonglin.
    - `priest_job`: an officiant that holds the communion ritual, and only while it's happy enough
      (`min_happiness`).
    - The upgrade to noble: a population requirement with a `scope`, so patricians must stay over
      half of the upper classes (patricians and nobles).

### Noble

The top of the settlement.

- **Needs**: a home (`noble_house`), cake, diamonds, beef, a flower garden, and a visit to the
  village square.
- **Jobs**: blesser.
- **Worth a look**:
    - `noble_house`: a house with a `lighting` requirement.
    - `flower_garden_need`: a decoration need.
    - `plaza_visit_need`: a visit to the `village_square`, a compound that needs three settler
      houses and an `obelisk` (a monument) around it.
    - Two downgrades, to patrician and to artisan.

## Examples outside the classes

Some files aren't used by any class, and are there only as examples:

- `basic_energy_need`: an energy need, which needs a mod with energy storage.
- `two_storey_house` and `two_storey_stairs_house`: multi-storey buildings, connected by a
  ladder or by stairs.

## Where to find an example of...

| Feature | File |
|---|---|
| Goods need with `hoarding` | `need/bread_need.json` |
| Fluid need | `need/water_need.json` |
| Energy need | `need/basic_energy_need.json` |
| Equipment need | `need/iron_sword_need.json` |
| Home need | `need/settler_home.json` |
| Structure need with `requires_usage` | `need/smithy_access_need.json` |
| Visit need with `consumes`, `use_supplies` and `requires_running` | `need/tavern_visit_need.json` |
| Social need | `need/settlers_companions_need.json` |
| Socialize need | `need/conversation_need.json` |
| Pet need | `need/cat_companion_need.json` |
| Lighting need | `need/high_lighting_need.json` |
| Decoration need | `need/flower_garden_need.json` |
| Altitude and biome needs | `need/high_altitude_need.json`, `need/mountain_biomes_need.json` |
| Ritual need | `need/communion_need.json` |
| Reading need | `need/reading_need.json` |
| Sleep need | `need/sleep_need.json` |
| Hearing need, as a `bonus` with an `icon` | `need/music_need.json` |
| `active_when` | `need/water_need.json`, `need/warmth_need.json` |
| `hidden` needs | `need/citizen_book_need.json` |
| Job `schedule` | `job/tavern_keeper_job.json` |
| Class `schedule` | `social_class/soldier.json` |
| `required_needs` and `min_happiness` on jobs | `job/guard_job.json`, `job/priest_job.json` |
| `available_to_babies` | `job/gather_flowers_job.json` |
| Population requirement with `scope` | `social_class/patrician.json` |
| `name_samples` | any social class |
| House with `lighting` | `structure/noble_house.json` |
| Enclosed building with its key block inside | `structure/bakery.json` |
| Multi-storey building | `structure/two_storey_house.json` |
| Open-air platform | `structure/farm.json` |
| Pasture | `structure/pasture.json` |
| Monument | `structure/obelisk.json` |
| Compound | `structure/village_square.json` |
