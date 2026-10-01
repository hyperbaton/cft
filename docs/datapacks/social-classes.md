# Social Class

Every Xoonglin belongs to a social class, which decides what it needs to be happy and what work
it can do: its needs, its possible jobs, its daily schedule and even the style of its name. Classes
are linked through upgrades and downgrades: a happy Xoonglin whose needs are well met can rise to a
higher class, and an unhappy one can fall to a lower one, as long as the population keeps the
proportions between classes that each class asks for. This way, a player's population grows from
a few settlers into a society whose higher classes need more, and finer, things. In game, press
`V` to see the classes of the loaded datapacks as a graph.

Each class is a file in `data/<namespace>/cft/social_class/`.

??? example "Sample social class file"

    ```json
    {
      "max_happiness": 100.0,
      "mating_happiness_threshold": 5.0,
      "spontaneously_spawn_population": 3,
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
          "next_class": "cft:citizen",
          "required_happiness": 20,
          "required_needs": [
            {
              "need": "cft:potato_need",
              "satisfaction_threshold": 0.70
            }
          ],
          "social_structure_requirements": [
            {
              "social_class": "cft:settler",
              "percentage": 0.30,
              "scope": ["cft:settler", "cft:citizen"]
            }
          ]
        }
      ],
      "downgrades": []
    }
    ```

- `max_happiness`: Happiness for a single Xoonglin of this class will not get greater
  than this.
- `mating_happiness_threshold`: The happiness value a Xoonglin needs to achieve to consider
  mating. Extra conditions may apply.
- `spontaneously_spawn_population`: The number of individuals of this class that will
  spawn (per player) if homes are available. Apart from these, they need to mate or come
  from other classes.
- `needs`: The list of needs, as references, for this class.
- `jobs`: _(Optional)_ A list of job references for this class. Each Xoonglin will randomly
  pick one of the listed jobs. If empty or omitted, the Xoonglin has no job.
- `max_health`: _(Optional, default: 20.0)_ The max health for Xoonglins of this class.
  Useful for making combat-oriented classes tougher.
- `can_upgrade_as_baby`: _(Optional, default: false)_ Whether baby Xoonglins of this class
  can upgrade.
- `can_downgrade_as_baby`: _(Optional, default: true)_ Whether baby Xoonglins of this class
  can downgrade.
- `mating_delay`: _(Optional, default: -1)_ Custom mating cooldown in ticks for this class.
  If -1, uses the global config value.
- `name_samples`: _(Optional)_ A list of example names. Xoonglins spawned or born into this
  class get a new name generated from them (with a Markov chain), so the names sound similar
  without just repeating the list. This lets each class have its own naming "culture" or
  language. With short lists the generated names mostly repeat the samples, so 30 or more
  are recommended. If omitted, the default Xoonglin-style names are used. A Xoonglin keeps
  its name when it changes class.
- `schedule`: _(Optional)_ The daily routine of this class: when its Xoonglins work, have
  free time and rest. See [Schedules](schedules.md). If omitted, they work from sunrise until
  their job's daily hours are done and are idle the rest of the time.
- `upgrades`: A list of ways a Xoonglin can become a higher class.
    - `next_class`: Reference to next class.
    - `required_happiness`: Minimum happiness level to consider upgrading.
    - `required_needs`: These needs have to be satisfied at the given value for the upgrade
      to be possible.
    - `social_structure_requirements`: A list of such requirements. Each social class mentioned
      must represent a percentage lower or equal to this one. Always in the range [0,1].
        - `scope`: _(Optional)_ A list of social class IDs. If given, the percentage is
          computed among only those classes' combined population, instead of the whole
          population. If omitted or empty, the percentage is of the whole population.
- `downgrades`: A list of ways a Xoonglin can become a lower class.
    - `next_class`: Reference to next class.
    - `required_happiness`: If happiness gets lower than this, the Xoonglin will downgrade.
    - `required_needs`: These needs have to be satisfied at the given value or the Xoonglin will
      downgrade.
    - `social_structure_requirements`: A list of such requirements. Each social class mentioned
      must represent a percentage higher or equal to this one. Always in the range [0,1].
        - `scope`: _(Optional)_ A list of social class IDs. If given, the percentage is
          computed among only those classes' combined population, instead of the whole
          population. If omitted or empty, the percentage is of the whole population.
