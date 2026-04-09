package net.bluelotuscoding.skillleveling.bridge.cnpc;

public class CnpcQuestMappingDef {
    public String title;
    public String bookCategory;
    public String classId;
    public String trackStructure;

    public String getNormalizedBookCategory() {
        return normalize(bookCategory, null);
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? fallback : normalized;
    }
}
