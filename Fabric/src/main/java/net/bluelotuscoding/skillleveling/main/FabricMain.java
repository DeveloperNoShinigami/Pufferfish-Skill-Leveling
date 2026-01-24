package net.bluelotuscoding.skillleveling.main;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.bluelotuscoding.skillleveling.network.TomeActionPacket;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Fabric main class for the Skill Leveling addon
 */
public class FabricMain implements ModInitializer {

        public static final Identifier TOME_ACTION_PACKET_ID = SkillLevelingMod.createIdentifier("tome_action");

        @Override
        public void onInitialize() {
                SkillLevelingMod.init();

                // Register items
                registerItems();

                // Register C2S packet handlers
                registerPacketHandlers();

                // Register commands
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> SkillLevelingCommand.register(dispatcher));

                ServerLifecycleEvents.SERVER_STARTING.register(server -> SkillLevelingMod.getInstance()
                                .getSkillLevelingManager().onServerStarting(server));
                ServerLifecycleEvents.SERVER_STOPPING.register(server -> SkillLevelingMod.getInstance()
                                .getSkillLevelingManager().onServerStopping(server));
                ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SkillLevelingMod.getInstance()
                                .getSkillLevelingManager().onPlayerJoin(handler.player));
                ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SkillLevelingMod.getInstance()
                                .getSkillLevelingManager().onPlayerLeave(handler.player));
        }

        private void registerPacketHandlers() {
                // Register C2S handler for Tome actions
                ServerPlayNetworking.registerGlobalReceiver(TOME_ACTION_PACKET_ID,
                                (server, player, handler, buf, responseSender) -> {
                                        TomeActionPacket packet = TomeActionPacket.read(buf);
                                        server.execute(() -> {
                                                SkillLevelingMod.getInstance().getSkillLevelingManager()
                                                                .processTomeAction(player, packet.getCategoryId(),
                                                                                packet.getSkillId(),
                                                                                packet.getTomeType());
                                        });
                                });
        }

        private void registerItems() {
                // Register Tome items
                ModItems.TOME_OF_PROGRESSION = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_PROGRESSION_ID,
                                ModItems.createTomeOfProgression());
                ModItems.TOME_OF_CLEAR_MIND = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_CLEAR_MIND_ID,
                                ModItems.createTomeOfClearMind());
                ModItems.TOME_OF_GREATER_CLEAR_MIND = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_GREATER_CLEAR_MIND_ID,
                                ModItems.createTomeOfGreaterClearMind());

                // Add to creative tab
                ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
                        content.add(ModItems.TOME_OF_PROGRESSION);
                        content.add(ModItems.TOME_OF_CLEAR_MIND);
                        content.add(ModItems.TOME_OF_GREATER_CLEAR_MIND);
                });
        }
}
