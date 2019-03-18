package processing.core.util.image.save;

import org.junit.Test;
import processing.core.PConstants;
import processing.core.util.common.CommonInputTestUtil;

import java.io.IOException;


public class TgaImageSaveStrategyTest {

  @Test
  public void testSave() throws IOException {
    TgaImageSaveStrategy strategy = new TgaImageSaveStrategy();
    int[] pixels = CommonInputTestUtil.generateTestPixels();

    strategy.save(
        pixels,
        7,
        5,
        PConstants.RGB,
        CommonInputTestUtil.getFullPath(
            "resource-test/scratch/test-image-writer.tga"
        )
    );

    CommonInputTestUtil.checkSavedFile("resource-test/scratch/test-image-writer.tga");
  }

}
