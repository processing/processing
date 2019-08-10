package processing.core;

import org.junit.Assert;
import org.junit.Test;
import processing.core.PApplet;
import processing.core.PImage;

import java.awt.*;


public class Base64StringImageLoadTest {

  // Simple pattern to try loading. Would be part of SVG.
  private static final String TEST_CONTENT = "data:image/svg;base64,iVBORw0KGgoAAAANSUhEUgAAAAcAAAAFCAMAAAC+RAbqAAAADFBMVEUAAAA0AP67u7v///+votL2AAAAGElEQVR4AWNgYAZBIIIAIJ8RyGGC8WHyAAYpAE9LN1znAAAAAElFTkSuQmCC";

  @Test
  public void testLoad() {
    PImage results = PShape.parseBase64Image(TEST_CONTENT);

    // Simply check a few sample pixels from the above pattern
    Assert.assertEquals(7, results.pixelWidth);
    Assert.assertEquals(5, results.pixelHeight);
    Assert.assertEquals(7, results.width);
    Assert.assertEquals(5, results.height);

    int valueBlack = results.get(0, 0);
    int valueBlue = results.get(2, 2);

    Assert.assertEquals(Color.BLACK, new Color(valueBlack));
    Assert.assertEquals(new Color(52, 0, 254), new Color(valueBlue));
  }

}
