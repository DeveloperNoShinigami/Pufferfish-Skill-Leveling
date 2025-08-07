package net.puffish.skillsmod.client;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.client.data.ClientSkillScreenData;
import net.puffish.skillsmod.client.network.ClientPacketSender;
import net.puffish.skillsmod.client.network.packets.in.ExperienceUpdateInPacket;
import net.puffish.skillsmod.client.network.packets.in.PointsUpdateInPacket;
import net.puffish.skillsmod.client.network.packets.in.ShowCategoryInPacket;
import net.puffish.skillsmod.client.network.packets.in.SkillUpdateInPacket;
import net.puffish.skillsmod.client.setup.ClientRegistrar;
import net.puffish.skillsmod.network.Packets;

public class SkillsClientMod {
        private static SkillsClientMod instance;

        private final ClientSkillScreenData screenData = new ClientSkillScreenData();

        private final ClientPacketSender packetSender;

        private SkillsClientMod(ClientPacketSender packetSender) {
                this.packetSender = packetSender;
        }

        public static SkillsClientMod getInstance() {
                return instance;
        }

        public static void setup(
                        ClientRegistrar registrar,
                        ClientPacketSender packetSender
        ) {
                instance = new SkillsClientMod(packetSender);

                registrar.registerInPacket(
                                Packets.SHOW_CATEGORY,
                                ShowCategoryInPacket::read,
                                instance::onShowCategory
                );

                registrar.registerInPacket(
                                Packets.SKILL_UPDATE,
                                SkillUpdateInPacket::read,
                                instance::onSkillUpdatePacket
                );

                registrar.registerInPacket(
                                Packets.POINTS_UPDATE,
                                PointsUpdateInPacket::read,
                                instance::onPointsUpdatePacket
                );

                registrar.registerInPacket(
                                Packets.EXPERIENCE_UPDATE,
                                ExperienceUpdateInPacket::read,
                                instance::onExperienceUpdatePacket
                );
        }

        private void onShowCategory(ShowCategoryInPacket packet) {
                var category = packet.getCategory();
                screenData.putCategory(category.getConfig().id(), category);
        }

        private void onSkillUpdatePacket(SkillUpdateInPacket packet) {
                screenData.getCategory(packet.getCategoryId()).ifPresent(category -> {
                        if (packet.isUnlocked()) {
                                category.unlock(packet.getSkillId(), packet.getLevel());
                        } else {
                                category.lock(packet.getSkillId());
                        }
                });
        }

        private void onExperienceUpdatePacket(ExperienceUpdateInPacket packet) {
                screenData.getCategory(packet.getCategoryId()).ifPresent(category -> {
                        category.setCurrentLevel(packet.getCurrentLevel());
                        category.setCurrentExperience(packet.getCurrentExperience());
                        category.setRequiredExperience(packet.getRequiredExperience());
                });
        }

        private void onPointsUpdatePacket(PointsUpdateInPacket packet) {
                screenData.getCategory(packet.getCategoryId()).ifPresent(category -> {
                        category.updatePoints(
                                        packet.getSpentPoints(),
                                        packet.getEarnedPoints()
                        );
                });
        }

        public ClientPacketSender getPacketSender() {
                return packetSender;
        }
}
