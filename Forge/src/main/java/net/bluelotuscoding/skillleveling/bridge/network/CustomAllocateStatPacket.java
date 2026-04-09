package net.bluelotuscoding.skillleveling.bridge.network;

import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.AttributeDef;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class CustomAllocateStatPacket {
    public final String statId;
    public final int points;

    public CustomAllocateStatPacket(String statId, int points) {
        this.statId = statId;
        this.points = points;
    }

    public static void encode(CustomAllocateStatPacket msg, PacketByteBuf buf) {
        buf.writeString(msg.statId);
        buf.writeInt(msg.points);
    }

    public static CustomAllocateStatPacket decode(PacketByteBuf buf) {
        return new CustomAllocateStatPacket(buf.readString(), buf.readInt());
    }

    public static void handle(CustomAllocateStatPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null) return;

            // Look up the slot definition to check for command slots
            AttributeDef slotDef = null;
            String cn = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform().getEpicClassName(player);
            var def = EpicClassConfigManager.getClassDef(cn);
            if (def != null) {
                outer: for (var page : EpicClassConfigManager.getPagesForClass(def.class_name)) {
                    if (page.slots != null) {
                        for (var sd : page.slots) {
                            if (msg.statId.equals(sd.id)) { slotDef = sd; break outer; }
                        }
                    }
                }
            }

            // First update ECM allocation so command-side refresh sees current values.
            EpicClassSyncHelper.allocateStat(player, msg.statId, msg.points);

            if (slotDef != null && slotDef.command != null) {
                try {
                    // Fire side-effect command as the player (e.g. KubeJS applyStats triggers)
                    int currentPoints = EpicClassSyncHelper.getStatPoints(player, msg.statId);
                    double v = 0.0;

                    // Expression mode: value is a formula using `points`.
                    if (slotDef.compiledExpression != null) {
                        v = slotDef.compiledExpression.eval(java.util.Map.of("points", (double) currentPoints));
                    }

                    // Numeric shorthand mode: value="1" means 1 per allocated point.
                    if (slotDef.value != null) {
                        String expr = slotDef.value.trim();
                        if (!expr.isEmpty() && !expr.contains("points")) {
                            try {
                                v = Double.parseDouble(expr) * currentPoints;
                            } catch (NumberFormatException ignored) {
                                // Keep expression-derived value when not a plain number.
                            }
                        }
                    }

                    if (currentPoints <= 0) {
                        v = 0.0;
                    }

                    String valStr = (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
                    String cmd = slotDef.command
                        .replace("{value}", valStr)
                        .replace("{player}", player.getEntityName());
                        // Run as player source so commands expecting ctx.source.player (e.g. /roleveling refresh) work.
                        var playerSource = player.getCommandSource().withSilent();
                        player.getServer().getCommandManager().executeWithPrefix(playerSource, cmd);
                } catch (Exception e) {
                    // Side-effects must never block stat allocation writes.
                    AddonLogger.LOGGER.error("[Bridge] Command side-effect failed for statId=" + msg.statId
                        + ": " + e.getMessage());
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    public void sendToServer() {
        net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.sendToServer(this);
    }
}
