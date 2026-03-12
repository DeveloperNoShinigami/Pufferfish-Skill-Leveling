package net.bluelotuscoding.skillleveling.bridge.forge;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;

import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementDef;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemRequirementForgeEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) {
                return;
            }

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            List<String> failures = ItemRequirementsManager.checkRequirements(player, itemId,
                    ItemRequirementsManager.TargetType.ITEM);

            if (!failures.isEmpty()) {
                event.setCanceled(true);
                player.sendMessage(Text.literal("§cRestricted (Item): " + failures.get(0)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity player) {
            String blockId = Registries.BLOCK.getId(event.getState().getBlock()).toString();
            List<String> failures = ItemRequirementsManager.checkRequirements(player, blockId,
                    ItemRequirementsManager.TargetType.BLOCK);

            if (!failures.isEmpty()) {
                event.setCanceled(true);
                player.sendMessage(Text.literal("§cRestricted (Block): " + failures.get(0)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            String blockId = Registries.BLOCK.getId(player.getWorld().getBlockState(event.getPos()).getBlock())
                    .toString();
            List<String> failures = ItemRequirementsManager.checkRequirements(player, blockId,
                    ItemRequirementsManager.TargetType.BLOCK);

            if (!failures.isEmpty()) {
                event.setCanceled(true);
                player.sendMessage(Text.literal("§cRestricted (Interaction): " + failures.get(0)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            // Check Weapon/Item first
            ItemStack stack = player.getMainHandStack();
            if (!stack.isEmpty()) {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                List<String> failures = ItemRequirementsManager.checkRequirements(player, itemId,
                        ItemRequirementsManager.TargetType.ITEM);
                if (!failures.isEmpty()) {
                    event.setCanceled(true);
                    player.sendMessage(Text.literal("§cRestricted (Weapon): " + failures.get(0)), true);
                    return;
                }
            }

            // Check Target Entity
            String entityId = Registries.ENTITY_TYPE.getId(event.getTarget().getType()).toString();
            List<String> failures = ItemRequirementsManager.checkRequirements(player, entityId,
                    ItemRequirementsManager.TargetType.ENTITY);
            if (!failures.isEmpty()) {
                event.setCanceled(true);
                player.sendMessage(Text.literal("§cRestricted (Entity): " + failures.get(0)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            String entityId = Registries.ENTITY_TYPE.getId(event.getTarget().getType()).toString();
            List<String> failures = ItemRequirementsManager.checkRequirements(player, entityId,
                    ItemRequirementsManager.TargetType.ENTITY);

            if (!failures.isEmpty()) {
                event.setCanceled(true);
                player.sendMessage(Text.literal("§cRestricted (Entity): " + failures.get(0)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            EquipmentSlot slot = event.getSlot();
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                return;
            }

            ItemStack stack = event.getTo();
            if (stack.isEmpty()) {
                return;
            }

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            List<String> failures = ItemRequirementsManager.checkRequirements(player, itemId,
                    ItemRequirementsManager.TargetType.ITEM);

            if (!failures.isEmpty()) {
                if (!player.getInventory().insertStack(stack.copy())) {
                    player.dropItem(stack.copy(), false);
                }
                player.equipStack(slot, ItemStack.EMPTY);
                player.sendMessage(Text.literal("§cRestricted (Armor): " + failures.get(0)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END
                || event.side != net.minecraftforge.fml.LogicalSide.SERVER) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        if (player.getWorld().getTime() % 20 != 0) {
            return;
        }

        // 1. Dimension Check
        String dimId = player.getWorld().getRegistryKey().getValue().toString();
        List<String> dimFailures = ItemRequirementsManager.checkRequirements(player, dimId,
                ItemRequirementsManager.TargetType.DIMENSION);
        if (!dimFailures.isEmpty()) {
            player.sendMessage(Text.literal("§cDimension Restricted: " + dimFailures.get(0)), true);
            ServerWorld overworld = player.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
            if (overworld != null) {
                BlockPos spawnPos = overworld.getSpawnPos();
                player.teleport(overworld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getYaw(),
                        player.getPitch());
            }
            return;
        }

        // 2. Structure/Dungeon Proximity (Area Check)
        ServerWorld world = (ServerWorld) player.getWorld();
        var structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        for (var entry : ItemRequirementsManager.getServerRequirementsMap().entrySet()) {
            ItemRequirementDef def = entry.getValue();
            if (def.structures != null) {
                for (String structId : def.structures) {
                    var structure = structureRegistry.get(new Identifier(structId));
                    if (structure != null) {
                        if (world.getStructureAccessor().getStructureAt(player.getBlockPos(), structure)
                                .hasChildren()) {
                            List<String> failures = ItemRequirementsManager.checkRequirements(player, structId,
                                    ItemRequirementsManager.TargetType.STRUCTURE);
                            if (!failures.isEmpty()) {
                                player.sendMessage(Text.literal("§cArea Restricted: " + failures.get(0)), true);
                                player.addVelocity(-player.getRotationVector().x * 1.5, 0.4,
                                        -player.getRotationVector().z * 1.5);
                                player.velocityModified = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
