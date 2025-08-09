package net.puffish.skillsmod.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.common.SkillConnection;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.config.skill.SkillConfig;
import net.puffish.skillsmod.config.skill.SkillConnectionsConfig;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
import net.puffish.skillsmod.config.skill.SkillDefinitionsConfig;
import net.puffish.skillsmod.config.skill.SkillsConfig;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.network.Packets;
import net.puffish.skillsmod.reward.builtin.PerLevelRewardsReward;
import net.puffish.skillsmod.server.data.CategoryData;

public record ShowCategoryOutPacket(CategoryConfig category, CategoryData categoryData) implements OutPacket {

    @Override
    public void write(PacketByteBuf buf) {
               buf.writeIdentifier(category.id());
               buf.writeBoolean(category.general().exclusiveRoot());
               buf.writeInt(category.general().spentPointsLimit());
               write(buf, category.definitions());
               write(buf, category.skills());
               write(buf, category.connections());
                buf.writeMap(
                                category.skills().getMap(),
                                PacketByteBuf::writeString,
                                (buf1, skill) -> {
                                        buf1.writeEnumConstant(
                                                        categoryData.getSkillState(
                                                                        category,
                                                                        skill,
                                                                        category.definitions().getById(skill.definitionId()).orElseThrow()
                                                        )
                                        );
                                        buf1.writeInt(categoryData.getSkillLevel(skill.id()));
                                }
                );
        buf.writeInt(categoryData.getSpentPoints(category));
        buf.writeInt(categoryData.getPointsTotal());
        category.experience().ifPresentOrElse(experience -> {
            buf.writeBoolean(true);
            var curve = experience.curve();
            buf.writeInt(curve.getLevelLimit());
            var progress = curve.getProgress(categoryData.getExperience());
            buf.writeInt(progress.currentLevel());
            buf.writeInt(progress.currentExperience());
            buf.writeInt(progress.requiredExperience());
        }, () -> buf.writeBoolean(false));
    }

    public void write(PacketByteBuf buf, SkillDefinitionsConfig definitions) {
        buf.writeCollection(definitions.getAll(), (buf1, definition) -> write(buf, definition));
    }

       public void write(PacketByteBuf buf, SkillDefinitionConfig definition) {
               buf.writeString(definition.id());
               buf.writeIdentifier(definition.type());
               buf.writeInt(definition.maxLevels());
               buf.writeInt(definition.cost());
               buf.writeInt(definition.requiredSkills());
               buf.writeInt(definition.requiredPoints());
               buf.writeInt(definition.requiredSpentPoints());
               buf.writeInt(definition.requiredExclusions());
               buf.writeBoolean(definition.rewards().stream().anyMatch(reward -> reward.type().equals(PerLevelRewardsReward.ID)));
       }

    public void write(PacketByteBuf buf, SkillsConfig skills) {
        buf.writeCollection(skills.getAll(), ShowCategoryOutPacket::write);
    }

    public void write(PacketByteBuf buf, SkillConnectionsConfig connections) {
        buf.writeCollection(connections.normal().getAll(), ShowCategoryOutPacket::write);
        buf.writeCollection(connections.exclusive().getAll(), ShowCategoryOutPacket::write);
    }

    public static void write(PacketByteBuf buf, SkillConfig skill) {
        buf.writeString(skill.id());
        buf.writeInt(skill.x());
        buf.writeInt(skill.y());
        buf.writeString(skill.definitionId());
        buf.writeBoolean(skill.isRoot());
    }

    public static void write(PacketByteBuf buf, SkillConnection skill) {
        buf.writeString(skill.skillAId());
        buf.writeString(skill.skillBId());
        buf.writeBoolean(skill.bidirectional());
    }


    @Override
    public Identifier getId() {
        return Packets.SHOW_CATEGORY;
    }
}
