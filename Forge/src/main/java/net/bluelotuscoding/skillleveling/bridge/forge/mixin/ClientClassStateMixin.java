package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.client.ClientCustomClassState;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.ClientClassState", remap = false)
public class ClientClassStateMixin {
    private static final Set<String> FAILED_CLASS_LOGS = new HashSet<>();

    private static String[] padArray(String[] raw, int len) {
        if (raw == null) {
            raw = new String[0];
        }
        String[] out = Arrays.copyOf(raw, len);
        for (int i = raw.length; i < len; ++i) {
            out[i] = "";
        }
        return out;
    }

    private static Object addon$getStaticField(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static int addon$getStaticInt(String className, String fieldName) {
        Object val = addon$getStaticField(className, fieldName);
        return val instanceof Integer ? (int) val : 0;
    }

    private static EpicClassDef getSelectedClassDef() {
        if (!EpicClassBridge.isEnabled()) {
            return null;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return null;
        }
        // ClientCustomClassState is authoritative. No proxy fallback — multiple classes
        // can share the same epic_class_proxy making reverse lookup non-deterministic.
        String customClassId = ClientCustomClassState.getCustomClass(mc.player.getUuid());
        if (customClassId != null && !"epic_classes:none".equals(customClassId)) {
            var res = EpicClassConfigManager.getClassDef(customClassId);
            if (res == null) {
                if (FAILED_CLASS_LOGS.add(customClassId)) {
                    net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                            .error("Failed to find class definition for ID: " + customClassId);
                }
            }
            return res;
        }
        return null;
    }

    private static boolean addon$isNone() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return true;
        }

        String customClassId = ClientCustomClassState.getCustomClass(mc.player.getUuid());
        if (customClassId == null || "epic_classes:none".equals(customClassId) || "none".equals(customClassId)) {
            Object selectedTypeObj = addon$getStaticField("com.example.epicclassmod.client.ClientClassState",
                    "selectedType");
            if (selectedTypeObj instanceof Enum) {
                return "NONE".equals(((Enum<?>) selectedTypeObj).name());
            }
            return true;
        }
        return false;
    }

    @Inject(method = "setClassOf", at = @At("TAIL"), remap = false)
    private static void addon$onSetClassOf(java.util.UUID id, @Coerce Object t, CallbackInfo ci) {
        net.bluelotuscoding.skillleveling.bridge.forge.ClientClassUIHelper.forceRefresh();
    }

    @Inject(method = "displayNameForSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private static void addon$overrideDisplayName(CallbackInfoReturnable<String> cir) {
        var def = getSelectedClassDef();
        if (def != null) {
            String name = (def.display_name != null && !def.display_name.isEmpty()) ? def.display_name
                    : (def.gui_title != null ? net.minecraft.client.resource.language.I18n.translate(def.gui_title)
                            : def.class_name);
            if (name != null && !name.isEmpty()) {
                cir.setReturnValue(name);
            }
        } else if (addon$isNone()) {
            cir.setReturnValue("");
        }
    }

    @Inject(method = "displayNameKeyForSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onDisplayNameKeyForSelected(CallbackInfoReturnable<String> cir) {
        EpicClassDef def = getSelectedClassDef();
        if (def != null) {
            String name = (def.display_name != null && !def.display_name.isEmpty()) ? def.display_name
                    : (def.gui_title != null ? net.minecraft.client.resource.language.I18n.translate(def.gui_title)
                            : def.class_name);
            if (name != null && !name.isEmpty()) {
                cir.setReturnValue(name);
            }
        } else if (addon$isNone()) {
            cir.setReturnValue("");
        }
    }

    @Inject(method = "notesForSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onNotesForSelected(CallbackInfoReturnable<String[]> cir) {
        EpicClassDef def = getSelectedClassDef();
        if (def != null) {
            String lore = def.book_lore != null ? def.book_lore : (def.description != null ? def.description : "");
            if (lore.isEmpty()) {
                cir.setReturnValue(new String[0]);
                return;
            }

            var client = MinecraftClient.getInstance();
            String translatedLore = net.minecraft.client.resource.language.I18n.translate(lore);
            var wrapped = client.textRenderer.getTextHandler().wrapLines(translatedLore, 140,
                    net.minecraft.text.Style.EMPTY);

            int noteLines = addon$getStaticInt("com.example.epicclassmod.client.ClientClassState", "NOTE_LINES");
            if (noteLines <= 0) {
                noteLines = 7;
            }

            String[] lines = new String[noteLines];
            for (int i = 0; i < noteLines; i++) {
                if (i < wrapped.size()) {
                    StringBuilder sb = new StringBuilder();
                    wrapped.get(i).visit(s -> {
                        sb.append(s);
                        return java.util.Optional.empty();
                    });
                    lines[i] = sb.toString();
                } else {
                    lines[i] = "";
                }
            }
            cir.setReturnValue(lines);
        } else if (addon$isNone()) {
            int noteLines = addon$getStaticInt("com.example.epicclassmod.client.ClientClassState", "NOTE_LINES");
            cir.setReturnValue(new String[noteLines > 0 ? noteLines : 7]);
        }
    }

    @Inject(method = "classWeaponEffectsForSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onClassWeaponEffectsForSelected(CallbackInfoReturnable<String[]> cir) {
        EpicClassDef def = getSelectedClassDef();
        int weaponEffectsCount = addon$getStaticInt("com.example.epicclassmod.client.ClientClassState",
                "CLASS_WEAPON_EFFECTS");
        if (weaponEffectsCount <= 0) {
            weaponEffectsCount = 4;
        }

        if (def != null && def.gui_passives != null) {
            String[] arr = new String[weaponEffectsCount];
            Arrays.fill(arr, "");
            for (int i = 0; i < def.gui_passives.size() && i < weaponEffectsCount; i++) {
                var passive = def.gui_passives.get(i);
                if (passive.pufferfish_skill_id != null && !passive.pufferfish_skill_id.isEmpty()) {
                    String[] display = EpicClassBridge.getSkillDisplay(def.skill_category_id,
                            passive.pufferfish_skill_id);
                    if (display != null) {
                        arr[i] = display[0];
                    } else {
                        arr[i] = passive.name_key != null
                                ? net.minecraft.client.resource.language.I18n.translate(passive.name_key)
                                : "";
                    }
                } else {
                    arr[i] = passive.name_key != null
                            ? net.minecraft.client.resource.language.I18n.translate(passive.name_key)
                            : "";
                }
            }
            cir.setReturnValue(arr);
        } else if (def != null) {
            cir.setReturnValue(padArray(new String[0], weaponEffectsCount));
        } else if (addon$isNone()) {
            cir.setReturnValue(padArray(new String[0], weaponEffectsCount));
        }
    }

    @Inject(method = "classWeaponEffectDescsForSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onClassWeaponEffectDescsForSelected(CallbackInfoReturnable<String[]> cir) {
        EpicClassDef def = getSelectedClassDef();
        int weaponEffectsCount = addon$getStaticInt("com.example.epicclassmod.client.ClientClassState",
                "CLASS_WEAPON_EFFECTS");
        if (weaponEffectsCount <= 0) {
            weaponEffectsCount = 4;
        }

        if (def != null && def.gui_passives != null) {
            String[] arr = new String[weaponEffectsCount];
            Arrays.fill(arr, "");
            for (int i = 0; i < def.gui_passives.size() && i < weaponEffectsCount; i++) {
                var passive = def.gui_passives.get(i);
                if (passive.pufferfish_skill_id != null && !passive.pufferfish_skill_id.isEmpty()) {
                    String[] display = EpicClassBridge.getSkillDisplay(def.skill_category_id,
                            passive.pufferfish_skill_id);
                    if (display != null) {
                        arr[i] = display[1];
                    } else {
                        arr[i] = passive.desc_key != null
                                ? net.minecraft.client.resource.language.I18n.translate(passive.desc_key)
                                : "";
                    }
                } else {
                    arr[i] = passive.desc_key != null
                            ? net.minecraft.client.resource.language.I18n.translate(passive.desc_key)
                            : "";
                }
            }
            cir.setReturnValue(arr);
        } else if (def != null) {
            cir.setReturnValue(padArray(new String[0], weaponEffectsCount));
        } else if (addon$isNone()) {
            cir.setReturnValue(padArray(new String[0], weaponEffectsCount));
        }
    }

    @Inject(method = "classWeaponLinesForSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onClassWeaponLinesForSelected(CallbackInfoReturnable<String[]> cir) {
        EpicClassDef def = getSelectedClassDef();
        if (def != null) {
            String weaponType = def.class_weapon_type;
            int weaponLinesCount = addon$getStaticInt("com.example.epicclassmod.client.ClientClassState",
                    "CLASS_WEAPON_LINES");
            if (weaponLinesCount <= 0) {
                weaponLinesCount = 2;
            }

            String[] arr = new String[weaponLinesCount];
            Arrays.fill(arr, "");

            String label = net.minecraft.client.resource.language.I18n.translate("gui.epicfight.exclusive_weapon");
            if ("gui.epicfight.exclusive_weapon".equals(label)) {
                label = "Class Exclusive Weapon";
            }
            arr[0] = label;
            if (weaponType != null && !weaponType.isEmpty()) {
                arr[1] = net.minecraft.client.resource.language.I18n.translate(weaponType);
            }
            cir.setReturnValue(arr);
        } else if (addon$isNone()) {
            int weaponLinesCount = addon$getStaticInt("com.example.epicclassmod.client.ClientClassState",
                    "CLASS_WEAPON_LINES");
            cir.setReturnValue(new String[weaponLinesCount > 0 ? weaponLinesCount : 2]);
        }
    }

    @Inject(method = "classWeaponItemsForSelected", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onClassWeaponItemsForSelected(CallbackInfoReturnable<String[]> cir) {
        EpicClassDef def = getSelectedClassDef();
        if (def != null) {
            java.util.List<String> list = new java.util.ArrayList<>();
            // Walk the parent chain so child classes inherit parent weapon restrictions
            EpicClassDef current = def;
            while (current != null) {
                if (current.class_weapon_icon != null && !current.class_weapon_icon.isEmpty()
                        && !list.contains(current.class_weapon_icon)) {
                    list.add(current.class_weapon_icon);
                }
                if (current.class_weapon_items != null) {
                    for (String item : current.class_weapon_items) {
                        if (!list.contains(item)) {
                            list.add(item);
                        }
                    }
                }
                // Advance to parent
                if (current.class_parent != null && !current.class_parent.isEmpty()) {
                    EpicClassDef parent = EpicClassConfigManager.getClassDef(current.class_parent);
                    // Avoid infinite loop if parent chain is circular
                    current = (parent != def) ? parent : null;
                } else {
                    current = null;
                }
            }
            if (!list.isEmpty()) {
                cir.setReturnValue(list.toArray(new String[0]));
            }
        } else if (addon$isNone()) {
            cir.setReturnValue(new String[0]);
        }
    }
}
