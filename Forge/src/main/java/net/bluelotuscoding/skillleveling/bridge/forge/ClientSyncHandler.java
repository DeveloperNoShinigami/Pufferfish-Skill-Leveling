package net.bluelotuscoding.skillleveling.bridge.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.MinecraftClient;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.client.ClientCustomClassState;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientSkillScreenData;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.minecraft.util.Identifier;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Handles client-side synchronization of XP/Level data from Pufferfish to Epic
 * Class Mod.
 * This ensures that the Epic Class HUD and Book UI reflect the Pufferfish
 * progress.
 */
@OnlyIn(Dist.CLIENT)
public class ClientSyncHandler {

    private boolean reflectionResolved = false;
    private boolean reflectionFailed = false;

    // Epic Class Reflection
    private Field fieldLevel;
    private Field fieldXp;
    private Field fieldXpNeeded;

    // Pufferfish Reflection
    private Field fieldScreenData;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !EpicClassBridge.isEnabled()) {
            return;
        }

        // 1. Resolve Reflection (Lazy)
        if (!reflectionResolved && !reflectionFailed) {
            resolveReflection();
        }
        if (reflectionFailed) {
            return;
        }

        try {
            // 3. Detect Active Class
            String detectedClass = ClientCustomClassState.getCustomClass(mc.player.getUuid());

            // 4. Sync XP/Level for the active class (Original logic)
            Optional<Identifier> categoryIdOpt = EpicClassBridge.getCategoryForClass(detectedClass);
            if (categoryIdOpt.isEmpty()) {
                return;
            }

            SkillsClientMod clientMod = SkillsClientMod.getInstance();
            ClientSkillScreenData screenData = (ClientSkillScreenData) fieldScreenData.get(clientMod);

            if (screenData == null) {
                return;
            }

            Optional<ClientCategoryData> categoryDataOpt = screenData.getCategory(categoryIdOpt.get());

            if (categoryDataOpt.isPresent()) {
                ClientCategoryData data = categoryDataOpt.get();

                int currentLevel = data.getCurrentLevel();
                int currentXp = data.getCurrentExperience();
                int neededXp = data.getRequiredExperience();

                fieldLevel.setInt(null, currentLevel);
                fieldXp.setInt(null, currentXp);

                // If neededXp is 0 or 100 (fallback), it's likely incorrect
                // In some cases Pufferfish returns 0 for max level or if data isn't ready
                if (neededXp <= 0) {
                    neededXp = 1000000; // High value to avoid "full bar" look if unknown
                }
                fieldXpNeeded.setInt(null, neededXp);
            }

        } catch (Exception e) {
            if (mc.player.age % 100 == 0) {
                SkillLevelingMod.getInstance().getLogger().error("[Bridge] Client Sync Error: " + e.getMessage());
            }
        }
    }

    private void resolveReflection() {
        try {
            // Epic Class Mod Reflection
            Class<?> levelStateClazz = Class.forName("com.example.epicclassmod.client.ClientLevelState");
            fieldLevel = levelStateClazz.getDeclaredField("level");
            fieldLevel.setAccessible(true);
            fieldXp = levelStateClazz.getDeclaredField("xp");
            fieldXp.setAccessible(true);
            fieldXpNeeded = levelStateClazz.getDeclaredField("xpNeeded");
            fieldXpNeeded.setAccessible(true);

            // Pufferfish Reflection
            fieldScreenData = SkillsClientMod.class.getDeclaredField("screenData");
            fieldScreenData.setAccessible(true);

            reflectionResolved = true;
            SkillLevelingMod.getInstance().getLogger().info("[Bridge] ClientSyncHandler reflection resolved.");
        } catch (Exception e) {
            reflectionFailed = true;
            SkillLevelingMod.getInstance().getLogger()
                    .error("[Bridge] Failed to resolve Client State reflection: " + e.getMessage());
        }
    }
}
