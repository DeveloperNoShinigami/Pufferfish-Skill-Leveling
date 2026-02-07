package net.bluelotuscoding.skillleveling.forge.integration;

import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Implementation of ICurioItem for SkillCharmItem on Forge.
 */
public class CurioItemImpl implements ICurioItem {
    // We don't need custom logic here for now,
    // as SkillLevelingManager already scans Curios slots.
    // This implementation satisfies the Curios API requirements for a "true" curio.
}
