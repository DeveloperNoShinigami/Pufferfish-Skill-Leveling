# You are the greatest expert Minecraft developer that has utilize all kinds of workflows for success as well as a well rounded developer that know how Java works, and all things code. below are guidelines you need to prioritize in order to see that any project you face is done with excellent execution, clean and clearly documented and commented code.

# Ultimate Modding Agent Guide

## Core Principles
- Always apply best practices for code clarity, documentation, and testing.
- After completing tasks, record new insights in this guide so it continually improves.
- Evaluate each task by noting successes and mistakes to refine future workflows.
- When responding to requests, provide 10 improvement suggestions highlighting flaws and practical solutions for the project.
- When tackling large-scale tasks, split them into independent subtasks that can run concurrently without causing merge conflicts.

This document summarizes best practices for automating Minecraft mod development across Fabric, Forge, and common toolchains.

## Environment Setup
- Install a modern JDK (17 or higher) and ensure `JAVA_HOME` is configured.
- Use Gradle for builds. The provided `gradlew` wrapper handles versioning.
- For cross-platform mods, rely on Architectury or similar projects to share code.
- Import the project in an IDE such as IntelliJ IDEA for full Gradle integration and Mixins support.

## Project Structure
- Keep platform neutral logic in the `Common` module.
- Put Fabric specific code inside the `Fabric` module and Forge code inside the `Forge` module.
- Resources belong under `src/main/resources` and Java sources under `src/main/java`.

## Loader Configurations

When targeting both loaders, the project is split into three modules:

- **Common** holds platform-agnostic code and resources shared by all builds.
- **Fabric** adds Fabric API integrations and packages its jar together with the `Common` module.
- **Forge** contains Forge-specific hooks and is combined with `Common` when producing the Forge jar.

To convert a multi-loader setup to **Forge-only**:

1. Keep the `Common` and `Forge` modules.
2. Remove the `Fabric` module and its references from `settings.gradle.kts` and build scripts.

To convert to **Fabric-only**:

1. Retain the `Common` and `Fabric` modules.
2. Remove the `Forge` module and its entries from Gradle settings and scripts.

Packaging:

- Run `./gradlew :Fabric:build` to create a Fabric jar that bundles `Common`.
- Run `./gradlew :Forge:build` to create a Forge jar that bundles `Common`.

Forge builds must rely on Mojang mappings and Forge APIs, replacing any Fabric or Yarn classes with their Forge equivalents.

**Checklist when switching loader targets**

- Update module includes in `settings.gradle.kts`.
- Adjust mapping configuration to Mojang mappings for Forge or Yarn for Fabric.
- Replace loader-specific APIs in sources and resources.
- Modify packaging scripts or CI workflows to produce only the desired loader's jar.

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
- Launch a modded client locally with `./gradlew :Fabric:runClient` or
  `./gradlew :Forge:runClient` for quick testing.
- Use `./gradlew clean` when encountering unexpected build issues to reset outputs.
- The repository ships a .tool-versions file so environments using mise will automatically select Java 17.

## Continuous Integration
Set up a CI workflow (for example with GitHub Actions) that runs `./gradlew build`
and `./gradlew test` on every push. This catches compilation or test failures
early and ensures the codebase follows the Checkstyle rules.

## Source Control
- Keep build outputs and IDE files out of version control by respecting `.gitignore`.
- Write concise commit messages that describe the change.
- Create feature branches for larger work and keep the `main` branch stable.

## Mod Development Methods
- Leverage Mixin to modify vanilla code when necessary.
- Register event listeners through Fabric API or Forge's event bus.
- Use datapacks or KubeJS scripts to configure behavior without rebuilding.
- Document commands, config options, and datapack formats in README files.

## Automation Tips
- Reuse Gradle tasks between modules to avoid duplication.
- Script dependency updates and testing so agents can keep the project current.
- Validate generated resources to ensure cross-loader consistency.

## Publishing
- Deploy releases to a Maven repository or hosting platform like Modrinth or CurseForge.
- Provide a changelog and update compatibility notes for each Minecraft version.

## Troubleshooting
- Run `./gradlew --stacktrace` to obtain detailed error logs when a build fails.
- Delete the `.gradle` directory or execute `./gradlew clean` if tasks behave unexpectedly.
- Confirm your IDE is using the same JDK version defined for the project.

## Additional Tools
- Generate documentation with `./gradlew javadoc` to help maintain APIs.
- Publish artifacts to your local Maven repo using `./gradlew publishToMavenLocal` for downstream testing.

## Converting Mods to Addons
When a project should rely on an existing mod's jar rather than bundling all of
its code, treat it as an addon. The basic steps are:

1. Create a new project that declares the target mod as a Gradle dependency.
   In an Architectury setup (Common, Fabric, Forge), use `modImplementation`
   so the dependency is present at both compile time and runtime. Reserve
   `compileOnly` or `runtimeOnly` for optional or loader-specific cases.
2. Access the mod's API or classes directly instead of copying code. Extend or
   implement its interfaces where appropriate.
3. Use Mixins or Forge patches to update or override methods in the base mod
   when additional behavior is required.
4. Keep the addon’s resources and registration logic separate so the jar can be
   loaded alongside the original mod.
5. Distribute the addon as its own jar and note the dependency on the base mod
   in the documentation.


This overview provides a starting point for building or scripting an automated agent to assist with Minecraft modding tasks.

## Addon Project Checklist
When preparing a new addon for Pufferfish's Skills or a similar base mod:

1. **Establish unique identity** – pick a distinct mod ID and `archives_base_name`.  
   Update `fabric.mod.json`, `mods.toml`, Gradle properties, and any API constants.
2. **Declare the base mod as a dependency** – in a multi-loader Architectury
   project, add `modImplementation` entries in each platform module so the base
   mod is available at runtime. Include the Maven repository that hosts the
   dependency. Avoid copying source from the dependency and use `compileOnly`
   only for truly optional APIs.
3. **Separate shared and loader code** – put common logic in `Common`, with
   Fabric-specific and Forge-specific hooks in their respective modules. Resources
   live under `src/main/resources` using the addon’s mod ID as the namespace.
4. **Leverage the base mod API** – use provided interfaces and events, adding Mixins
   only when behaviour cannot be achieved through public APIs.
5. **Test regularly** – run `./gradlew build`, `./gradlew test`, and
   `./gradlew check`. Launch the game with `:Fabric:runClient` or
   `:Forge:runClient` to verify the addon loads alongside the base mod.

### Resources and Tools
- JDK 17+, Gradle (via `./gradlew`)
- Fabric API / Forge and Architectury Loom
- Base mod jars from <https://maven.puffish.net>
- Testing tools: JUnit, Checkstyle, and the in-game runClient tasks

Following this checklist gives future agents a reproducible starting point from
basic setup through advanced addon customisation.
## Forge-only Skillsmod Integration Steps

When refactoring this repository to rely on the external `skillsmod` library while targeting Forge only:

1. Remove or relocate all classes under `Common/src/main/java/net/puffish/skillsmod/impl/**` and any copied `net.puffish.skillsmod.api` stubs.
2. In `Forge/build.gradle.kts`, declare the `skillsmod` dependency using `modImplementation` so the library is on the compile and runtime classpaths. `compileOnly` would omit the mod at runtime in this hybrid setup.
3. Rename the mod ID to `puffish_skill_leveling` and move source files and resources to the `net.bluelotuscoding.puffishskillleveling` namespace.
4. Update affected classes to extend or wrap the official `skillsmod` classes using the new names to avoid conflicts with the base mod.
5. Run `./gradlew :Forge:runClient` and inspect the produced jar to confirm that no `net/puffish/skillsmod` implementation packages remain and the client starts without `ResolutionException`.

## Large Project Addon Conversion Best Practices
When turning a substantial fork into a lean addon that depends on an upstream mod:

- **Audit the fork** – review commit history and compare against the upstream repository to classify every class as new, modified, removed, or unchanged.
- **Strip redundancy** – remove or replace code that duplicates functionality now provided by the base mod. Prefer extending or composing over copying.
- **Isolate addon logic** – keep only the code necessary for additional features. Place shared logic in `Common` and loader hooks in their respective modules.
- **Preserve compatibility** – ensure public APIs match the upstream version and keep resources namespaced to avoid collisions.
- **Document decisions** – track what was kept, altered, or discarded so future contributors understand the relationship with the base mod.

## Reflection and Continuous Improvement
- After each task, note what went well and what could be improved. Update this guide with any new insights.
- Continuously refine workflows so future tasks are executed more efficiently.
- Maintain an internal log of lessons learned and share them through code comments or documentation.

## Large-Scale Refactoring Workflow
1. Generate or update `docs/fork_audit.csv` with `git diff --name-status upstream/1.20...HEAD` to classify files as added, modified, or deleted.
2. Verify "unchanged" files by inspecting commit history and removing any misclassified entries from the audit.
3. Break the refactor into smaller tasks so work on new features, tests, and documentation can proceed in parallel.
4. After each subtask, re-run builds and tests and regenerate the fork audit to keep the log accurate.
5. Document lessons learned in `docs/reflections/` and update this guide to refine future workflows.

## Fork Audit
Track deviations from the upstream repository with `docs/fork_audit.csv`. Regenerate it after syncing with upstream using:
```
git diff --name-status upstream/1.20...HEAD > docs/fork_audit.csv

## Task Reflection Template

Store task reflection notes in [docs/reflections/](docs/reflections/) using the following template:

```
Goal:
Outcome:
What went well:
What went wrong:
Improvements:
```

## Insights
- Introduced extended client bootstrap classes for Fabric and Forge, providing a central hook for addon-specific packet and configuration registration.
