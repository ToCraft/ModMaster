# Contributing to ModMaster

ModMaster is the Gradle plugin that drives compilation, versioning, and publishing for all [ToCraft](https://tocraft.dev) Minecraft mods. Because every mod in the ecosystem depends on it, changes here have wide-ranging downstream effects — please read this guide before opening a pull request.

## Table of Contents

- [Role in the Ecosystem](#role-in-the-ecosystem)
- [Ways to Contribute](#ways-to-contribute)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Making Changes](#making-changes)
- [Versioning and Publishing](#versioning-and-publishing)
- [CI / GitHub Actions](#ci--github-actions)
- [Downstream Impact](#downstream-impact)
- [License](#license)

---

## Role in the Ecosystem

ModMaster sits at the top of the ToCraft dependency chain:

```
ModMaster  →  every mod by ToCraft
```

Sometimes, a new ModMaster release is required before any of its dependents can be updated to a new Minecraft version. Keep this in mind when proposing changes — even a small breaking change may require coordinated updates across multiple repositories.

There are two versions of ModMaster: `single` and `multi`. The main difference is that `single` is intended for a typical multi-loader mod setup, while `multi` is setup to support multiple Minecraft versions **and** multiple mod loaders with just one codebase. Because this was very difficult to maintain over time, **only** `single` is still maintained!

---

## Ways to Contribute

- **Bug reports** – Open an [issue](https://github.com/ToCraft/ModMaster/issues) with a clear description, the ModMaster version, and a minimal reproduction if possible.
- **Bug fixes & improvements** – Fork the repo, make your changes, and open a pull request against the `single` branch.
- **Financial support** – [Patreon](https://www.patreon.com/tocraft).

---

## Development Setup

### Prerequisites

| Tool | Minimum version |
|------|----------------|
| JDK  | 25             |
| Git  | any recent     |

IntelliJ IDEA with the Kotlin plugin and the Gradle plugin is recommended, as the plugin itself is written in Kotlin.

### Cloning and building

```bash
git clone https://github.com/ToCraft/ModMaster.git
cd ModMaster

# The default branch is 'single'
git checkout single

./gradlew build
```

### Testing changes locally

Because ModMaster is consumed by other projects as a Gradle plugin, the most effective way to test a change is to publish it to your local Maven cache and point a downstream project (e.g. CraftedCore or Woodwalkers) at that local version.

```bash
# 1. Publish to local Maven cache
./gradlew publishToMavenLocal

# 2. In a downstream project's build.gradle.kts, resolve the plugin from mavenLocal():
#    settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        // ...
    }
}

# 3. Bump the plugin version in the downstream project to match
id("dev.tocraft.modmaster.root") version ("2.X.Y-SNAPSHOT")
```

---

## Project Structure

```
ModMaster/
├── src/
│   └── main/
│       └── kotlin/dev/tocraft/modmaster/   # Plugin source (Kotlin)
├── build.gradle.kts                        # Plugin build definition
├── gradle.properties                       # Plugin version (e.g. version=2.2)
└── settings.gradle.kts
```

The plugin is published to `https://maven.tocraft.dev/public` under the group `dev.tocraft` and applied in downstream projects as `dev.tocraft.modmaster.root`.

### Key dependencies bundled by the plugin

ModMaster bundles and exposes the following to consumer projects:

| Dependency | Purpose |
|-----------|---------|
| `net.fabricmc.fabric-loom` | Fabric mod compilation |
| `net.neoforged.moddev` | NeoForge mod compilation |
| `net.darkhax.curseforgegradle` | CurseForge publishing |
| `com.modrinth.minotaur` | Modrinth publishing |
| `com.diluv.schoomp` | Discord webhook notifications |

If you need to update one of these transitive dependencies, update the version in `build.gradle.kts` and verify the change doesn't break the downstream build.

---

## Making Changes

1. Fork the repository and create a feature branch off `single`.
2. Make your changes in `src/main/kotlin/dev/tocraft/modmaster/`.
3. Build and test locally (see above).
4. **If** you changed the variable naming for `gradle.properties`, update [modmaster-build-action](https://github.com/ToCraft/modmaster-build-action) and [modmaster-release-action](https://github.com/ToCraft/modmaster-release-action) to match the new naming.
4. Open a **pull request** against the `single` branch with a clear description of what changed and why.

Since ModMaster has downstream consumers, please be conservative with breaking API changes. If a change is necessarily breaking, note it prominently in the PR description so the maintainer can plan coordinated updates to CraftedCore and the mods.

---

## Versioning and Publishing

The plugin version is set in `gradle.properties`:

```properties
version=2.2
```

---

## CI / GitHub Actions

ModMaster is used in two reusable composite actions maintained in the ToCraft organisation for the CI of all downstream mods.

### `modmaster-build-action` ([repo](https://github.com/ToCraft/modmaster-build-action))

Triggered on every push and pull request. It:

1. Checks out the repository.
2. Sets up the requested JDK (default: 25, via Temurin distribution).
3. Configures Gradle via `gradle/actions/setup-gradle`.
4. Runs `./gradlew check build`.
5. Reads `modid` and `mod_version` from `gradle.properties` and uploads the compiled jars as a named artifact (`<modid>-<mod_version>`).

Example usage in a downstream workflow:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: ToCraft/modmaster-build-action@v1.1
        with:
          java-version: "25"   # optional, defaults to 25
```

### `modmaster-release-action` ([repo](https://github.com/ToCraft/modmaster-release-action))

Triggered on releases. It:

1. Sets up the JDK and Gradle (same as the build action).
2. Extracts `artifact_type`, `modid`, and `mod_version` from `gradle.properties`.
3. Runs `./gradlew check build release`, publishing to CurseForge, Modrinth, the ToCraft Maven, and sending a Discord webhook notification — all driven by secrets passed as inputs.
4. Uploads the final jars as a GitHub Actions artifact (excluding dev, shadow, and testmod jars).
5. Creates a GitHub Release (only when `artifact_type=release`) using the extracted `CHANGELOG.md` as the body.

Required secrets for the release action:

| Input | Purpose |
|-------|---------|
| `maven-pass` | ToCraft Maven credentials |
| `curseforge-token` | CurseForge upload token |
| `modrinth-token` | Modrinth upload token |
| `webhook` | Discord webhook URL for release notifications |

Example usage:

```yaml
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: ToCraft/modmaster-release-action@v1.2
        with:
          maven-pass: ${{ secrets.MAVEN_PASS }}
          curseforge-token: ${{ secrets.CURSEFORGE_TOKEN }}
          modrinth-token: ${{ secrets.MODRINTH_TOKEN }}
          webhook: ${{ secrets.DISCORD_WEBHOOK }}
```

If you are improving either action, open a PR against the respective repository (`modmaster-build-action` or `modmaster-release-action`). Changes to those actions take effect for all downstream mods the next time they reference the updated tag.

---

## Downstream Impact

Any merged change to ModMaster should be followed by:

1. **A new ModMaster release** — bump `version` in `gradle.properties` and tag.
2. **A CraftedCore update** — update the ModMaster plugin version in CraftedCore's `build.gradle.kts` and verify it compiles.
3. **A mod update** — update the plugin version and confirm the full build works end to end.

This order is strict. Attempting to update a downstream project before the ModMaster release is published will result in a dependency resolution failure or worse.

---

## License

ModMaster is licensed under the [**Crafted License 1.0 by ToCraft**](https://github.com/ToCraft/modmaster-release-action/blob/main/LICENSE.md). By submitting a pull request you agree that your contribution will be made available under the same license.