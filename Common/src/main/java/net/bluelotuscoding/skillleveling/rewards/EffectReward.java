package net.bluelotuscoding.skillleveling.rewards;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;

public class EffectReward implements Reward {
    public static final Identifier ID = new Identifier("puffish_skills", "effect");

    private final Identifier effectId;
    private final int amplifier;
    private final int duration;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final boolean persistent;
    private final boolean isProtected;

    public EffectReward(Identifier effectId, int amplifier, int duration, boolean ambient, boolean showParticles,
            boolean showIcon, boolean persistent, boolean isProtected) {
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.duration = duration;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.persistent = persistent;
        this.isProtected = isProtected;
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        StatusEffect effect = Registries.STATUS_EFFECT.get(effectId);

        if (effect == null) {
            return;
        }

        if (context.getCount() > 0) {
            // Apply effect
            // If duration is -1, use a very large value (Integer.MAX_VALUE)
            int effectiveDuration = duration == -1 ? Integer.MAX_VALUE : duration;
            StatusEffectInstance instance = new StatusEffectInstance(effect, effectiveDuration, amplifier, ambient,
                    showParticles, showIcon);

            if (persistent) {
                // Remove curative items (like milk) to make the effect persistent
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform().makePersistent(instance);
            }

            player.addStatusEffect(instance);

            if (isProtected) {
                // Register for tick-based re-application
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getSkillLevelingManager()
                        .registerProtectedEffect(player.getUuid(),
                                new net.bluelotuscoding.skillleveling.manager.SkillLevelingManager.ProtectedEffect(
                                        effectId, amplifier, duration, ambient, showParticles, showIcon, persistent));
            }
        } else {
            if (isProtected) {
                // Unregister from tick-based re-application BEFORE removal
                // This prevents the event hook (LivingEntityMixin) from re-applying it
                // immediately
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getSkillLevelingManager()
                        .unregisterProtectedEffect(player.getUuid(), effectId);
            }

            // Remove effect
            player.removeStatusEffect(effect);
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
    }

    public static void register() {
        SkillsAPI.registerReward(ID, EffectReward::parse);
    }

    static Result<EffectReward, Problem> parse(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(obj -> parse(obj, context), context));
    }

    static Result<EffectReward, Problem> parse(JsonObject rootObject, ConfigContext context) {
        var problems = new ArrayList<Problem>();

        var effectIdStr = rootObject.getString("effect").ifFailure(problems::add).getSuccess();
        var amplifier = rootObject.get("amplifier").getSuccess()
                .flatMap(e -> e.getAsInt().ifFailure(problems::add).getSuccess()).orElse(0);
        var duration = rootObject.get("duration").getSuccess()
                .flatMap(e -> e.getAsInt().ifFailure(problems::add).getSuccess()).orElse(-1);
        var ambient = rootObject.get("ambient").getSuccess()
                .flatMap(e -> e.getAsBoolean().ifFailure(problems::add).getSuccess()).orElse(false);
        var showParticles = rootObject.get("show_particles").getSuccess()
                .flatMap(e -> e.getAsBoolean().ifFailure(problems::add).getSuccess()).orElse(true);
        var showIcon = rootObject.get("show_icon").getSuccess()
                .flatMap(e -> e.getAsBoolean().ifFailure(problems::add).getSuccess()).orElse(true);
        var persistent = rootObject.get("persistent").getSuccess()
                .flatMap(e -> e.getAsBoolean().ifFailure(problems::add).getSuccess()).orElse(false);
        var isProtected = rootObject.get("is_protected").getSuccess()
                .flatMap(e -> e.getAsBoolean().ifFailure(problems::add).getSuccess()).orElse(false);

        if (problems.isEmpty()) {
            Identifier effectId = new Identifier(effectIdStr.orElse("minecraft:haste"));
            return Result.success(new EffectReward(effectId, amplifier, duration, ambient, showParticles, showIcon,
                    persistent, isProtected));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }
}
