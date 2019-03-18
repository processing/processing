package processing.core.util.image.load;

import org.junit.Assert;
import org.junit.Test;


public class ImageLoadUtilTest {

  @Test
  public void testCheckExtensionRequiresAlphaTrue() {
    Assert.assertTrue(ImageLoadUtil.checkExtensionRequiresAlpha("png"));
  }

  @Test
  public void testCheckExtensionRequiresAlphaFalse() {
    Assert.assertFalse(ImageLoadUtil.checkExtensionRequiresAlpha("jpg"));
  }

}
