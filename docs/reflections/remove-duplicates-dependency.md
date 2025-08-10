Goal:
Remove duplicate classes and depend on upstream skillsmod.
Outcome:
Duplicate classes removed; gradle configured to pull base mod.
What went well:
Systematic removal and gradle updates were straightforward.
What went wrong:
Dependency resolution may fail if upstream artifact is unavailable.
Improvements:
Verify repository coordinates and add tests once upstream is accessible.
