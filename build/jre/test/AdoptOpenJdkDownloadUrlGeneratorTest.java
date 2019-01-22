import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;

public class AdoptOpenJdkDownloadUrlGeneratorTest {

  private static final String EXPECTED_WIN64_URL = "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_windows_hotspot_11.0.1_13.zip";
  private static final String EXPECTED_MAC_URL = "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_mac_hotspot_11.0.1_13.tar.gz";
  private static final String EXPECTED_LINUX_URL = "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_linux_hotspot_11.0.1_13.tar.gz";

  private static final boolean JDK = true;
  private static final int TRAIN = 11;
  private static final int VERSION = 0;
  private static final int UPDATE = 1;
  private static final int BUILD = 13;
  private static final String FLAVOR_SUFFIX = "-x64.tar.gz";
  private static final String HASH = "";

  private AdoptOpenJdkDownloadUrlGenerator urlGenerator;

  @Before
	public void setUp() throws Exception {
		urlGenerator = new AdoptOpenJdkDownloadUrlGenerator();
	}

  @Test
  public void testBuildUrlWindows() {
    String url = urlGenerator.buildUrl(
      "windows64",
      JDK,
      TRAIN,
      VERSION,
      UPDATE,
      BUILD,
      "windows" + FLAVOR_SUFFIX,
      HASH
    );

    assertEquals(
      EXPECTED_WIN64_URL,
      url
    );
  }

  @Test
  public void testBuildUrlMac() {
    String url = urlGenerator.buildUrl(
      "macos",
      JDK,
      TRAIN,
      VERSION,
      UPDATE,
      BUILD,
      "mac" + FLAVOR_SUFFIX,
      HASH
    );

    assertEquals(
      EXPECTED_MAC_URL,
      url
    );
  }

  @Test
  public void testBuildUrlLinux() {
    String url = urlGenerator.buildUrl(
      "linux64",
      JDK,
      TRAIN,
      VERSION,
      UPDATE,
      BUILD,
      "linux64" + FLAVOR_SUFFIX,
      HASH
    );

    assertEquals(
      EXPECTED_LINUX_URL,
      url
    );
  }

}
