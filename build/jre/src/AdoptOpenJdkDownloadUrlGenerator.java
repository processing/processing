/**
 * Utility to generate
 */
public class AdoptOpenJdkDownloadUrlGenerator extends DownloadUrlGenerator {

    // "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_aarch64_linux_hotspot_11.0.1_13.tar.gz";
    private static final String BASE_URL = "https://github.com/AdoptOpenJDK/openjdk%d-binaries/releases/download/jdk-%d.%d.%d%2B%d/OpenJDK%dU-%s_%d.%d.%d_%d.tar.gz";

    @Override
    public String buildUrl(String platform, boolean jdk, int train, int version, int update, int build, String flavor, String hash) {
        String filename = buildDownloadRemoteFilename(platform);
        return String.format(
                BASE_URL,
                train,
                train,
                version,
                update,
                build,
                filename,
                train,
                version,
                update,
                build
        );
    }

    private String buildDownloadRemoteFilename(String downloadPlatform) {
        switch (downloadPlatform.toLowerCase()) {
            case "windows32": return "jdk_x86-32_windows_hotspot";
            case "windows64": return "jdk_x64_windows_hotspot";
            case "macos": return "jdk_x64_mac_hotspot";
            case "linux32": throw new RuntimeException("Linux 32bit no longer supported by AdoptOpenJDK.");
            case "linux64": return "jdk_x64_linux_hotspot";
            case "linuxArm": return "jdk_aarch64_linux_hotspot";
            default: throw new RuntimeException("Unknown platform: " + downloadPlatform);
        }
    }
}
