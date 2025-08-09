package net.bluelotuscoding.puffishskillleveling.client.network.packets.in;

import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.api.Skill;
import net.bluelotuscoding.puffishskillleveling.client.config.ClientBackgroundConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.ClientCategoryConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.ClientFrameConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.ClientIconConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.colors.ClientColorConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.colors.ClientColorsConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.colors.ClientConnectionsColorsConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.colors.ClientFillStrokeColorsConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.skill.ClientSkillConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.skill.ClientSkillConnectionConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.skill.ClientSkillDefinitionConfig;
import net.bluelotuscoding.puffishskillleveling.client.data.ClientCategoryData;
import net.bluelotuscoding.puffishskillleveling.common.BackgroundPosition;
import net.bluelotuscoding.puffishskillleveling.network.InPacket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShowCategoryInPacket implements InPacket {
	private final ClientCategoryData category;

	private ShowCategoryInPacket(ClientCategoryData category) {
		this.category = category;
	}

	public static ShowCategoryInPacket read(PacketByteBuf buf) {
		var category = readCategory(buf);

		return new ShowCategoryInPacket(category);
	}

        public static ClientCategoryData readCategory(PacketByteBuf buf) {
               var id = buf.readIdentifier();
               var exclusiveRoot = buf.readBoolean();
               var spentPointsLimit = buf.readInt();

               var title = Text.empty();
               var icon = new ClientIconConfig.ItemIconConfig(new ItemStack(Items.AIR));
               var background = defaultBackground();
               var colors = defaultColors();

               var definitions = buf.readList(ShowCategoryInPacket::readDefinition)
                                .stream()
                                .collect(Collectors.toMap(ClientSkillDefinitionConfig::id, definition -> definition));

		var skills = buf.readList(ShowCategoryInPacket::readSkill)
				.stream()
				.collect(Collectors.toMap(ClientSkillConfig::id, skill -> skill));

		var normalConnections = buf.readList(ShowCategoryInPacket::readSkillConnection);
		var exclusiveConnections = buf.readList(ShowCategoryInPacket::readSkillConnection);

                var skillsInfo = buf.readMap(
                                PacketByteBuf::readString,
                                buf1 -> new SkillInfo(
                                                buf1.readEnumConstant(Skill.State.class),
                                                buf1.readInt()
                                )
                );

                var skillsStates = skillsInfo.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().state()));
                var skillLevels = skillsInfo.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().level()));

                var spentPoints = buf.readInt();
                var earnedPoints = buf.readInt();

		var levelLimit = Integer.MAX_VALUE;
		var currentLevel = Integer.MIN_VALUE;
		var currentExperience = Integer.MIN_VALUE;
		var requiredExperience = Integer.MIN_VALUE;
		if (buf.readBoolean()) {
			levelLimit = buf.readInt();
			currentLevel = buf.readInt();
			currentExperience = buf.readInt();
			requiredExperience = buf.readInt();
		}

		var category = new ClientCategoryConfig(
				id,
				title,
				icon,
				background,
				colors,
				exclusiveRoot,
				spentPointsLimit,
				levelLimit,
				definitions,
				skills,
				normalConnections,
				exclusiveConnections
		);

                return new ClientCategoryData(
                                category,
                                skillsStates,
                                skillLevels,
                                spentPoints,
                                earnedPoints,
                                currentLevel,
                                currentExperience,
                                requiredExperience
                );
        }

        private record SkillInfo(Skill.State state, int level) { }

       public static ClientSkillDefinitionConfig readDefinition(PacketByteBuf buf) {
               var id = buf.readString();
               var type = buf.readIdentifier();
               var maxLevels = buf.readInt();
               var cost = buf.readInt();
               var requiredSkills = buf.readInt();
               var requiredPoints = buf.readInt();
               var requiredSpentPoints = buf.readInt();
               var requiredExclusions = buf.readInt();
               var hasLevelRewards = buf.readBoolean();

               return new ClientSkillDefinitionConfig(
                               id,
                               type,
                               maxLevels,
                               List.of(),
                               List.of(),
                               Text.empty(),
                               new ClientIconConfig.ItemIconConfig(new ItemStack(Items.AIR)),
                               new ClientFrameConfig.AdvancementFrameConfig(AdvancementFrame.TASK),
                               1f,
                               false,
                               cost,
                               requiredSkills,
                               requiredPoints,
                               requiredSpentPoints,
                               requiredExclusions,
                               hasLevelRewards
               );
       }

       private static ClientBackgroundConfig defaultBackground() {
               return ClientBackgroundConfig.create(
                               new Identifier("minecraft", "textures/gui/options_background.png"),
                               256,
                               256,
                               BackgroundPosition.NONE
               );
       }

       private static ClientColorsConfig defaultColors() {
               var zero = new ClientColorConfig(0);
               var fillStroke = new ClientFillStrokeColorsConfig(zero, zero);
               var connections = new ClientConnectionsColorsConfig(
                               fillStroke,
                               fillStroke,
                               fillStroke,
                               fillStroke,
                               fillStroke
               );
               return new ClientColorsConfig(connections, fillStroke);
       }

	public static ClientSkillConfig readSkill(PacketByteBuf buf) {
		var id = buf.readString();
		var x = buf.readInt();
		var y = buf.readInt();
		var definition = buf.readString();
		var isRoot = buf.readBoolean();

		return new ClientSkillConfig(id, x, y, definition, isRoot);
	}

	public static ClientSkillConnectionConfig readSkillConnection(PacketByteBuf buf) {
		var skillAId = buf.readString();
		var skillBId = buf.readString();
		var bidirectional = buf.readBoolean();

		return new ClientSkillConnectionConfig(skillAId, skillBId, bidirectional);
	}

	public ClientCategoryData getCategory() {
		return category;
	}
}
