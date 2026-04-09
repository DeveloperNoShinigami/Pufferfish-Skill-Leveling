package net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc;

import java.util.List;

public record CnpcDialogView(
        int dialogId,
        int entityId,
        String npcName,
        String title,
        String speakerLine,
        String body,
        boolean hasQuest,
        String questId,
        String questTitle,
        List<Option> options) {

    public record Option(int slot, String text, int color) {
    }
}
