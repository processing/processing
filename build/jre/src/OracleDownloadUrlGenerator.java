/**
 * Utility to generate
 */
public class OracleDownloadUrlGenerator extends DownloadUrlGenerator{

    @Override
    public String buildUrl(String platform, boolean jdk, int train, int version, int update, int build, String flavor,
        String hash) {

        String filename = getLocalFilename(platform, jdk, train, version, update, build, flavor, hash);

        String url = "http://download.oracle.com/otn-pub/java/jdk/" +
                (update == 0 ?
                        String.format("%d-b%02d/", version, build) :
                        String.format("%du%d-b%02d/", version, update, build));

        // URL format changed starting with 8u121
        if (update >= 121) {
            url += hash + "/";
        }

        // Finally, add the filename to the end
        url += filename;

        return url;
    }

}
