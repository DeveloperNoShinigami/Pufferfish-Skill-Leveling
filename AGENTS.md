# Ultimate Modding Agent Guide

This document summarizes best practices for automating Minecraft mod development across Fabric, Forge, and common toolchains.

## Environment Setup
- Install a modern JDK (17 or higher) and ensure `JAVA_HOME` is configured.
- Use Gradle for builds. The provided `gradlew` wrapper handles versioning.
- For cross-platform mods, rely on Architectury or similar projects to share code.

## Project Structure
- Keep platform neutral logic in the `Common` module.
- Put Fabric specific code inside the `Fabric` module and Forge code inside the `Forge` module.
- Resources belong under `src/main/resources` and Java sources under `src/main/java`.

## Coding Practices
- Follow Java best practices and the repository's style conventions.
- Prefer composition over inheritance for complex systems.
- Add meaningful comments for non-trivial logic.
- Keep feature flags or version checks platform specific.

## Build and Testing
- Use `./gradlew build` to compile all modules.
- Execute `./gradlew test` to run unit tests.
- Run `./gradlew check` to perform code style analysis using Checkstyle. The rules
  come from `config/checkstyle/checkstyle.xml` and are applied to each module.
- Generated artifacts are found under `build/libs` for each platform.

## Continuous Integration
Set up a CI workflow (for example with GitHub Actions) that runs `./gradlew build`
and `./gradlew test` on every push. This catches compilation or test failures
early and ensures the codebase follows the Checkstyle rules.

## Mod Development Methods
- Leverage Mixin to modify vanilla code when necessary.
- Register event listeners through Fabric API or Forge's event bus.
- Use datapacks or KubeJS scripts to configure behavior without rebuilding.
- Document commands, config options, and datapack formats in README files.

## Publishing
- Deploy releases to a Maven repository or hosting platform like Modrinth or CurseForge.
- Provide a changelog and update compatibility notes for each Minecraft version.

## Converting Mods to Addons
When a project should rely on an existing mod's jar rather than bundling all of
its code, treat it as an addon. The basic steps are:

1. Create a new project that declares the target mod as a Gradle dependency
   (using `modImplementation`, `compileOnly`, or `runtimeOnly` depending on the
   loader).
2. Access the mod's API or classes directly instead of copying code. Extend or
   implement its interfaces where appropriate.
3. Use Mixins or Forge patches to update or override methods in the base mod
   when additional behavior is required.
4. Keep the addon’s resources and registration logic separate so the jar can be
   loaded alongside the original mod.
5. Distribute the addon as its own jar and note the dependency on the base mod
   in the documentation.


This overview provides a starting point for building or scripting an automated agent to assist with Minecraft modding tasks.
