# Combined branch

This branch bundles two RuneLite plugins so they run in one dev-mode client:

- **DMM Flipper** (this repository)
- **Harbourmaster** by Wouter Rutgers (BSD 2-Clause, see LICENSE-harbourmaster), copied from
  https://github.com/wouterrutgers/harbourmaster at commit ece2179 (v1.5.1).
  Sources are under `src/main/java/com/harbourmaster`, data under `src/main/resources/com/harbourmaster`.

## Run

- IntelliJ: run `com.dmmflipper.CombinedLauncher.main`
- Gradle: `./gradlew runCombined`

## Local changes to Harbourmaster

- `task-rewards.tsv` row 8784 (Port Khazard courier 6, Ardougne sand delivery) set to 130 XP, which is what the game pays.
  Upstream has 331.

To pick up upstream Harbourmaster changes, copy over the same folders from its repo and re-apply the change above.
