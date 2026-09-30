# Schedules

A schedule tells Xoonglins what to do at each time of the day. It can be set on a social class, and a
job can set its own, which replaces the class one for the Xoonglins with that job (e.g. guards
working the night shift). Xoonglins without a schedule keep the default behavior: they work
from sunrise until their daily hours are done, and are idle the rest of the time.

??? example "Sample schedule"

    ```json
    {"schedule": [
      { "at": 6, "activity": "minecraft:work" },
      { "at": 16, "activity": "minecraft:idle" },
      { "at": 21, "activity": "minecraft:rest" }
    ]}
    ```

The schedule is a list of transitions: at each one, the Xoonglin switches to a new activity,
which lasts until the next transition. The last transition of the day lasts until the first
one of the next day, so above, rest goes from 21:00 to 6:00.

- `at`: Clock hour of the transition, from 0 to 24, where 6 is sunrise (the start of the
  Minecraft day) and 18 is sunset. Decimals are allowed (e.g. 6.5 is 6:30).
- `activity`: A vanilla activity ID. Xoonglins understand these:
    - `minecraft:work`: the Xoonglin's job runs and counts towards its daily hours. The work
      time must add up to at least the job's `hours_per_day`, or the daily quota can never be met.
    - `minecraft:rest`: the Xoonglin goes back home and stays inside. If it has a
      [Sleep Need](needs.md#sleep-need) that needs satisfying, it sleeps in a free bed of its home.
      Attending a ritual still interrupts rest.
    - Any other activity, such as `minecraft:idle`, is free time: no job, the Xoonglin strolls
      around, tends to its needs and can mate.

Schedules are ignored in dimensions without a day cycle, such as the Nether.
