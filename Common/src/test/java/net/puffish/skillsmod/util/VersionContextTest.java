package net.puffish.skillsmod.util;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.calculation.operation.OperationConfigContextImpl;
import net.puffish.skillsmod.impl.experience.source.ExperienceSourceConfigContextImpl;
import net.puffish.skillsmod.impl.rewards.RewardConfigContextImpl;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

public class VersionContextTest {
    private static class DummyContext implements ConfigContext {
        @Override
        public MinecraftServer getServer() {
            return null;
        }

        @Override
        public void emitWarning(String message) {
        }
    }

    private static class DummyVersionedContext extends DummyContext implements VersionContext {
        private final OptionalInt version;

        DummyVersionedContext(int version) {
            this.version = OptionalInt.of(version);
        }

        @Override
        public OptionalInt getVersion() {
            return version;
        }
    }

    private static final Result<JsonElement, Problem> DATA = Result.failure(Problem.message("no data"));

    @Test
    public void experienceSourceMissingVersion() {
        var ctx = new ExperienceSourceConfigContextImpl(new DummyContext(), DATA);
        assertTrue(ctx.getVersion().isEmpty());
    }

    @Test
    public void experienceSourceWithVersion() {
        var ctx = new ExperienceSourceConfigContextImpl(new DummyVersionedContext(5), DATA);
        assertEquals(OptionalInt.of(5), ctx.getVersion());
    }

    @Test
    public void rewardMissingVersion() {
        var ctx = new RewardConfigContextImpl(new DummyContext(), DATA);
        assertTrue(ctx.getVersion().isEmpty());
    }

    @Test
    public void rewardWithVersion() {
        var ctx = new RewardConfigContextImpl(new DummyVersionedContext(7), DATA);
        assertEquals(OptionalInt.of(7), ctx.getVersion());
    }

    @Test
    public void operationMissingVersion() {
        var ctx = new OperationConfigContextImpl(new DummyContext(), DATA);
        assertTrue(ctx.getVersion().isEmpty());
    }

    @Test
    public void operationWithVersion() {
        var ctx = new OperationConfigContextImpl(new DummyVersionedContext(9), DATA);
        assertEquals(OptionalInt.of(9), ctx.getVersion());
    }
}
