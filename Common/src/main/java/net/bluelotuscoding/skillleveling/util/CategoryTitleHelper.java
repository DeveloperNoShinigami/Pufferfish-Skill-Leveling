package net.bluelotuscoding.skillleveling.util;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper to retrieve category titles directly from Pufferfish Skills
 * configurations.
 * This ensures titles are parsed from the datapack (category.json) as
 * requested.
 * 
 * DESIGN: Uses reflection to access internal Pufferfish configurations on both
 * client and server, with robust error handling to prevent startup crashes.
 */
public class CategoryTitleHelper {

    private static final Map<String, Text> titleCache = new ConcurrentHashMap<>();

    /**
     * Get the display title for a category.
     * Works on both client (from ClientCategoryConfig) and server (from
     * CategoryConfig).
     */
    public static Text getCategoryTitle(String categoryIdStr) {
        if (titleCache.containsKey(categoryIdStr)) {
            return titleCache.get(categoryIdStr);
        }

        Identifier id = new Identifier(categoryIdStr);

        Text title = null;

        // Try client-side first (most common for tooltips)
        try {
            title = getClientTitle(id);
        } catch (Throwable t) {
            // Log if needed, but stay silent to avoid spam during startup
        }

        // If not on client or not found, try server-side
        if (title == null) {
            try {
                title = getServerTitle(id);
            } catch (Throwable t) {
                // Log if needed
            }
        }

        // Fallback to Title Case ID
        if (title == null) {
            title = Text.literal(toTitleCase(id.getPath()));
        }

        titleCache.put(categoryIdStr, title);
        return title;
    }

    private static Text getClientTitle(Identifier id) throws Exception {
        try {
            // Use reflection to access net.puffish.skillsmod.client.SkillsClientMod
            Class<?> clientModClass = Class.forName("net.puffish.skillsmod.client.SkillsClientMod");
            java.lang.reflect.Method getInstance = clientModClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);

            if (instance == null)
                return null;

            java.lang.reflect.Field screenDataField = clientModClass.getDeclaredField("screenData");
            screenDataField.setAccessible(true);
            Object screenData = screenDataField.get(instance);

            if (screenData == null)
                return null;

            // Try to find getCategory method with either Identifier or ResourceLocation
            java.lang.reflect.Method getCategory = null;
            try {
                // Try Identifier first (Common/Fabric context)
                getCategory = screenData.getClass().getMethod("getCategory", net.minecraft.util.Identifier.class);
            } catch (NoSuchMethodException e) {
                // Fallback to ResourceLocation (Forge context)
                try {
                    Class<?> resLocClass = Class.forName("net.minecraft.resources.ResourceLocation");
                    getCategory = screenData.getClass().getMethod("getCategory", resLocClass);
                } catch (Exception e2) {
                    // Final attempt: Scan methods by name
                    for (java.lang.reflect.Method m : screenData.getClass().getMethods()) {
                        if (m.getName().equals("getCategory") && m.getParameterCount() == 1) {
                            getCategory = m;
                            break;
                        }
                    }
                }
            }

            if (getCategory == null)
                return null;

            Optional<?> optCategory = (Optional<?>) getCategory.invoke(screenData, id);

            if (optCategory != null && optCategory.isPresent()) {
                Object categoryData = optCategory.get();
                Object config = categoryData.getClass().getMethod("getConfig").invoke(categoryData);
                if (config != null) {
                    return (Text) config.getClass().getMethod("title").invoke(config);
                }
            }
        } catch (ClassNotFoundException e) {
            // Safe to ignore on servers
        }
        return null;
    }

    private static Text getServerTitle(Identifier id) throws Exception {
        try {
            Class<?> modClass = Class.forName("net.puffish.skillsmod.SkillsMod");
            Object instance = modClass.getMethod("getInstance").invoke(null);

            if (instance == null)
                return null;

            // Try to find getCategory method
            java.lang.reflect.Method getCategory = null;
            try {
                getCategory = modClass.getDeclaredMethod("getCategory", net.minecraft.util.Identifier.class);
            } catch (NoSuchMethodException e) {
                try {
                    Class<?> resLocClass = Class.forName("net.minecraft.resources.ResourceLocation");
                    getCategory = modClass.getDeclaredMethod("getCategory", resLocClass);
                } catch (Exception e2) {
                    for (java.lang.reflect.Method m : modClass.getDeclaredMethods()) {
                        if (m.getName().equals("getCategory") && m.getParameterCount() == 1) {
                            getCategory = m;
                            break;
                        }
                    }
                }
            }

            if (getCategory == null)
                return null;

            getCategory.setAccessible(true);
            Optional<?> optConfig = (Optional<?>) getCategory.invoke(instance, id);

            if (optConfig != null && optConfig.isPresent()) {
                Object config = optConfig.get();
                // config is likely a Record or Class with general() returning GeneralConfig
                Object general = config.getClass().getMethod("general").invoke(config);
                if (general != null) {
                    return (Text) general.getClass().getMethod("title").invoke(general);
                }
            }
        } catch (ClassNotFoundException e) {
            // Should not happen if mod is loaded
        }
        return null;
    }

    private static String toTitleCase(String id) {
        if (id == null || id.isEmpty())
            return id;
        String[] words = id.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}
