package net.puffish.skill_leveling.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.common.FrameType;
import net.puffish.skill_leveling.common.IconType;
import net.puffish.skill_leveling.common.SkillConnection;
import net.puffish.skill_leveling.config.BackgroundConfig;
import net.puffish.skill_leveling.config.CategoryConfig;
import net.puffish.skill_leveling.config.FrameConfig;
import net.puffish.skill_leveling.config.GeneralConfig;
import net.puffish.skill_leveling.config.IconConfig;
import net.puffish.skill_leveling.config.colors.ColorConfig;
import net.puffish.skill_leveling.config.colors.ColorsConfig;
import net.puffish.skill_leveling.config.colors.ConnectionsColorsConfig;
import net.puffish.skill_leveling.config.colors.FillStrokeColorsConfig;
import net.puffish.skill_leveling.config.skill.SkillConfig;
import net.puffish.skill_leveling.config.skill.SkillConnectionsConfig;
import net.puffish.skill_leveling.config.skill.SkillDefinitionConfig;
import net.puffish.skill_leveling.config.skill.SkillDefinitionsConfig;
import net.puffish.skill_leveling.config.skill.SkillsConfig;
import net.puffish.skill_leveling.network.OutPacket;
import net.puffish.skill_leveling.network.Packets;
import net.puffish.skill_leveling.server.data.CategoryData;

public record ShowCategoryOutPacket(CategoryConfig category, CategoryData categoryData) implements OutPacket {

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeIdentifier(category.id());
		write(buf, category.general());
		write(buf, category.definitions());
		write(buf, category.skills());
		write(buf, category.connections());
		buf.writeMap(
				category.skills().getMap(),
				PacketByteBuf::writeString,
				(buf1, skill) -> buf1.writeEnumConstant(
						categoryData.getSkillState(
								category,
								skill,
								category.definitions().getById(skill.definitionId()).orElseThrow()
						)
				)
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

	public void write(PacketByteBuf buf, GeneralConfig general) {
		buf.writeText(general.title());
		write(buf, general.icon());
		write(buf, general.background());
		write(buf, general.colors());
		buf.writeBoolean(general.exclusiveRoot());
		buf.writeInt(general.spentPointsLimit());
	}

	public void write(PacketByteBuf buf, SkillDefinitionConfig definition) {
		buf.writeString(definition.id());
		buf.writeIdentifier(definition.type());
		buf.writeInt(definition.maxLevels());
		buf.writeCollection(definition.descriptions(), PacketByteBuf::writeText);
		buf.writeCollection(definition.extraDescriptions(), PacketByteBuf::writeText);
		buf.writeText(definition.title());
		write(buf, definition.frame());
                write(buf, definition.icon());
                buf.writeFloat(definition.size());
                buf.writeBoolean(definition.mergeDescription());
                buf.writeInt(definition.cost());
		buf.writeInt(definition.requiredSkills());
		buf.writeInt(definition.requiredPoints());
		buf.writeInt(definition.requiredSpentPoints());
		buf.writeInt(definition.requiredExclusions());
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

	public static void write(PacketByteBuf buf, IconConfig icon) {
		if (icon instanceof IconConfig.EffectIconConfig effectIcon) {
			buf.writeEnumConstant(IconType.EFFECT);
			buf.writeIdentifier(Registries.STATUS_EFFECT.getId(effectIcon.effect()));
		} else if (icon instanceof IconConfig.ItemIconConfig itemIcon) {
			buf.writeEnumConstant(IconType.ITEM);
			buf.writeItemStack(itemIcon.item());
		} else if (icon instanceof IconConfig.TextureIconConfig textureIcon) {
			buf.writeEnumConstant(IconType.TEXTURE);
			buf.writeIdentifier(textureIcon.texture());
		}
	}

	public static void write(PacketByteBuf buf, FrameConfig frame) {
		if (frame instanceof FrameConfig.AdvancementFrameConfig advancementFrame) {
			buf.writeEnumConstant(FrameType.ADVANCEMENT);
			buf.writeEnumConstant(advancementFrame.frame());
		} else if (frame instanceof FrameConfig.TextureFrameConfig textureFrame) {
			buf.writeEnumConstant(FrameType.TEXTURE);
			buf.writeOptional(textureFrame.lockedTexture(), PacketByteBuf::writeIdentifier);
			buf.writeIdentifier(textureFrame.availableTexture());
			buf.writeOptional(textureFrame.affordableTexture(), PacketByteBuf::writeIdentifier);
			buf.writeIdentifier(textureFrame.unlockedTexture());
			buf.writeOptional(textureFrame.excludedTexture(), PacketByteBuf::writeIdentifier);
		}
	}

	public static void write(PacketByteBuf buf, BackgroundConfig background) {
		buf.writeIdentifier(background.texture());
		buf.writeInt(background.width());
		buf.writeInt(background.height());
		buf.writeEnumConstant(background.position());
	}

	public static void write(PacketByteBuf buf, ColorsConfig colors) {
		write(buf, colors.connections());
		write(buf, colors.points());
	}

	public static void write(PacketByteBuf buf, ConnectionsColorsConfig connectionsColors) {
		write(buf, connectionsColors.locked());
		write(buf, connectionsColors.available());
		write(buf, connectionsColors.affordable());
		write(buf, connectionsColors.unlocked());
		write(buf, connectionsColors.excluded());
	}

	public static void write(PacketByteBuf buf, FillStrokeColorsConfig fillStrokeColors) {
		write(buf, fillStrokeColors.fill());
		write(buf, fillStrokeColors.stroke());
	}

	public static void write(PacketByteBuf buf, ColorConfig color) {
		buf.writeInt(color.argb());
	}

	@Override
	public Identifier getId() {
		return Packets.SHOW_CATEGORY;
	}
}
