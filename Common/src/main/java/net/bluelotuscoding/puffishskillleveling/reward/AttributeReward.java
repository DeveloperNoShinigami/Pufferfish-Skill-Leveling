package net.bluelotuscoding.puffishskillleveling.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

/**
 * Reward that adds attribute modifiers to players.
 */
public class AttributeReward implements Reward {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("attribute");

    private final List<UUID> uuids = new ArrayList<>();
    private final Attribute attribute;
    private final float value;
    private final Operation operation;

    private AttributeReward(Attribute attribute, float value, Operation operation) {
        this.attribute = attribute;
        this.value = value;
        this.operation = operation;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, AttributeReward::parse);
    }

    private static Result<AttributeReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(AttributeReward::parse, context));
    }

    private static Result<AttributeReward, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        Optional<Attribute> optAttribute = rootObject.get("attribute")
                .andThen(element -> BuiltinJson.parseAttribute(element).andThen(attribute -> {
                    if (DefaultAttributes.getSupplier(EntityType.PLAYER).hasAttribute(attribute)) {
                        return Result.success(attribute);
                    }
                    return Result.failure(element.getPath().createProblem("Expected a valid player attribute"));
                }))
                .ifFailure(problems::add)
                .getSuccess();

        Optional<Float> optValue = rootObject.getFloat("value")
                .ifFailure(problems::add)
                .getSuccess();

        Optional<Operation> optOperation = rootObject.get("operation")
                .andThen(BuiltinJson::parseAttributeOperation)
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            return Result.success(new AttributeReward(
                    optAttribute.orElseThrow(),
                    optValue.orElseThrow(),
                    optOperation.orElseThrow()));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    private void createMissingUUIDs(int count) {
        while (uuids.size() < count) {
            uuids.add(UUID.randomUUID());
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        int count = context.getCount();
        AttributeInstance instance = Objects.requireNonNull(context.getPlayer().getAttribute(attribute));
        createMissingUUIDs(count);
        for (int i = 0; i < uuids.size(); i++) {
            UUID uuid = uuids.get(i);
            if (instance.getModifier(uuid) == null) {
                if (i < count) {
                    instance.addTransientModifier(new AttributeModifier(uuid, "", value, operation));
                }
            } else if (i >= count) {
                instance.removeModifier(uuid);
            }
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            AttributeInstance instance = Objects.requireNonNull(player.getAttribute(attribute));
            for (UUID uuid : uuids) {
                instance.removeModifier(uuid);
            }
        }
        uuids.clear();
    }
}
