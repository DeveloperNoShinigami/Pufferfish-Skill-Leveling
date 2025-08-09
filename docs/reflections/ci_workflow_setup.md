Goal:
Set up GitHub Actions CI that builds, tests, and checks both Fabric and Forge modules with cached Gradle dependencies.

Outcome:
Workflow added under `.github/workflows/ci.yml` and repository documentation updated.

What went well:
- Created matrix workflow covering both loaders.
- Implemented Gradle dependency caching.
- Updated AGENTS guide with CI information.

What went wrong:
- Initial attempt at editing AGENTS.md failed due to missing `ed` utility.

Improvements:
- Use a dedicated patch tool or built-in editor for editing documentation files.
