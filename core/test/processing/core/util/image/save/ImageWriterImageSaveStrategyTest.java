package processing.core.util.image.save;

import org.junit.Test;
import processing.core.PConstants;
import processing.core.util.common.CommonInputTestUtil;

import java.io.IOException;


public class ImageWriterImageSaveStrategyTest {

  @Test
  public void testSave() throws IOException {
    ImageWriterImageSaveStrategy strategy = new ImageWriterImageSaveStrategy();
    int[] pixels = CommonInputTestUtil.generateTestPixels();

    strategy.save(
        pixels,
        7,
        5,
        PConstants.RGB,
        CommonInputTestUtil.getFullPath(
            "resource-test/scratch/test-image-writer.png"
        )
    );

    CommonInputTestUtil.checkSavedFile("resource-test/scratch/test-image-writer.png");
  }

}
