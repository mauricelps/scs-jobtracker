package at.kitsoft.jobtracker.main;

import java.util.UUID;

public class TrackerMetaData {

    private int userId;
    private final int majorVersion = 1, minorVersion = 0, patchVersion = 25;
    private final String appId = "EnJGIcxU4WFffpa-lgJ<nb]j>pSYhFQK";
    private UUID installationId;

    public String getVersion() {
        return majorVersion + "." + minorVersion + "." + patchVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getPatchVersion() {
        return patchVersion;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public UUID getInstallationId() {
        return installationId;
    }

    public void setInstallationId(UUID installationId) {
        this.installationId = installationId;
    }

    public String getAppId() {
        return appId;
    }
}