# Agent Instructions for Pufferfish Skill Leveling

This repository hosts a Fabric/Forge cross platform Minecraft mod written in Java and built with Gradle.
These rules apply to any automated agent (such as OpenAI Codex) contributing to this project.

## Minecraft version support
This mod targets recent versions of Minecraft (1.20 and newer) but may be ported
to older releases when needed. When adding features or bug fixes:
- Keep shared logic in the `Common` module whenever possible.
- Add version-specific code only in the `Fabric` or `Forge` modules.
- Check the appropriate mappings and API changes for the target version before
  submitting code.
- For legacy versions prior to 1.12, consider using separate branches or forks
  as modern build scripts may not support them directly.

## Repository layout
- `Common`, `Fabric` and `Forge` contain the core and platform-specific code.
  - Java source lives in `src/main/java` and resources in `src/main/resources`.
  - Tests are under `src/test/java`.
- `example-skill-level-template.zip` demonstrates datapack usage.
- `README.md` documents datapack formats and mod features.

## Coding guidelines
- Use Java for mod code and follow the existing style (spaces for indentation and meaningful names).
- Keep platform specific code inside the appropriate submodule.
- Add comments when implementing complex logic.
- For datapack JSON or KubeJS scripts, mirror the style found in existing examples.
- Do not modify generated files such as the example datapack zip.

## Programmatic checks
- Run `./gradlew test` to execute unit tests.
- Run `./gradlew build` to ensure the mod compiles for all platforms.
All checks should succeed before submitting a pull request.

## Pull request guidelines
1. Provide a clear description of changes and reference related issues if applicable.
2. Keep PRs focused on a single topic (e.g., a bug fix or feature).
3. Ensure tests pass and mention any new datapacks or KubeJS scripts added.

These guidelines are intended to help automated agents contribute safely and effectively when working on Minecraft modding tasks, KubeJS scripting, datapacks, or other mod related features.
