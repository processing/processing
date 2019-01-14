/**
 * Interface for strategy to generate download URLs.
 */
public abstract class DownloadUrlGenerator {

    public abstract String buildUrl(String platform, boolean jdk, int train, int version, int update, int build,
        String flavor, String hash);

    public String getLocalFilename(String platform, boolean jdk, int train, int version, int update, int build,
        String flavor, String hash) {

        String baseFilename = (jdk ? "jdk" : "jre");

        String versionStr;
        if (update == 0) {
            versionStr = String.format("-%d-%s", version, flavor);
        } else {
            versionStr = String.format("-%du%d-%s", version, update, flavor);
        }

        return baseFilename + versionStr;
    }

}
