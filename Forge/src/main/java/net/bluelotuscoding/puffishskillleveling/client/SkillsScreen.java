package net.bluelotuscoding.puffishskillleveling.client;

import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.client.data.ClientCategoryData;

import java.util.Map;
import java.util.Optional;

/**
 * Wrapper around the base Skills screen that allows the addon to
 * reside in its own package while still delegating rendering to the
 * original implementation provided by the Skills mod.
 */
public class SkillsScreen extends net.puffish.skillsmod.client.gui.SkillsScreen {
    public SkillsScreen(Map<ResourceLocation, ClientCategoryData> categories,
                        Optional<ResourceLocation> openCategory) {
        super(categories, openCategory);
    }
}
