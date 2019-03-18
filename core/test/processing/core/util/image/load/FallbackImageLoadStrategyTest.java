package processing.core.util.image.load;

import org.junit.Assert;
import org.junit.Test;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.common.CommonInputTestUtil;


public class FallbackImageLoadStrategyTest {

  @Test
  public void testLoadFirst() {
    ImageLoadStrategy strategy1 = (applet, path, ext) -> new PImage(5, 7);
    ImageLoadStrategy strategy2 = (applet, path, ext) -> new PImage(7, 5);

    FallbackImageLoadStrategy fallbackImageLoadStrategy = new FallbackImageLoadStrategy(
        strategy1,
        strategy2
    );

    PApplet testPApplet = CommonInputTestUtil.getFakePApplet();
    PImage testImage = fallbackImageLoadStrategy.load(testPApplet, "test", "png");

    Assert.assertEquals(5, testImage.width);
  }

  @Test
  public void testLoadSecond() {
    ImageLoadStrategy strategy1 = (applet, path, ext) -> {
      throw new RuntimeException("test exception");
    };

    ImageLoadStrategy strategy2 = (applet, path, ext) -> new PImage(7, 5);

    FallbackImageLoadStrategy fallbackImageLoadStrategy = new FallbackImageLoadStrategy(
        strategy1,
        strategy2
    );

    PApplet testPApplet = CommonInputTestUtil.getFakePApplet();
    PImage testImage = fallbackImageLoadStrategy.load(testPApplet, "test", "png");

    Assert.assertEquals(7, testImage.width);
  }

}
