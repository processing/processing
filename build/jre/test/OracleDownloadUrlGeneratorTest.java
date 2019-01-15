import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;

public class OracleDownloadUrlGeneratorTest {

    private static final String EXPECTED_URL = "http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-macosx-x64.dmg";

    private OracleDownloadUrlGenerator urlGenerator;

    @Before
	public void setUp() throws Exception {
		urlGenerator = new OracleDownloadUrlGenerator();
	}

    @Test
    public void testBuildUrl() {
        String url = urlGenerator.buildUrl(
            "macos",
            true,
            1,
            8,
            131,
            11,
            "macosx-x64.dmg",
            "d54c1d3a095b4ff2b6607d096fa80163"
        );

        assertEquals(
            EXPECTED_URL,
            url
        );
    }

}
