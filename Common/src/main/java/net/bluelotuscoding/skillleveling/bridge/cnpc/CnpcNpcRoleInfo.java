package net.bluelotuscoding.skillleveling.bridge.cnpc;

public class CnpcNpcRoleInfo {
    private final String jobMasterClassId;
    private final String questNpcRoleId;

    public CnpcNpcRoleInfo(String jobMasterClassId, String questNpcRoleId) {
        this.jobMasterClassId = normalize(jobMasterClassId);
        this.questNpcRoleId = normalize(questNpcRoleId);
    }

    public String getJobMasterClassId() {
        return jobMasterClassId;
    }

    public String getQuestNpcRoleId() {
        return questNpcRoleId;
    }

    public boolean isJobMaster() {
        return jobMasterClassId != null;
    }

    public boolean isQuestNpc() {
        return questNpcRoleId != null;
    }

    public boolean hasAnyRole() {
        return isJobMaster() || isQuestNpc();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
