package processing.core.util.image.load;

import org.junit.Test;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.common.CommonInputTestUtil;

public class Base64StringImageLoadStrategyTest {

  private static final String TEST_CONTENT = "iVBORw0KGgoAAAANSUhEUgAAAAcAAAAFCAMAAAC+RAbqAAAADFBMVEUAAAA0AP67u7v///+votL2AAAAGElEQVR4AWNgYAZBIIIAIJ8RyGGC8WHyAAYpAE9LN1znAAAAAElFTkSuQmCC";

  @Test
  public void testLoad() {
    PApplet testApplet = CommonInputTestUtil.getFakePApplet();
    Base64StringImageLoadStrategy strategy = new Base64StringImageLoadStrategy();

    PImage result = strategy.load(testApplet, TEST_CONTENT, "png");

    CommonInputTestUtil.assertPImagePattern(result);
  }

}
