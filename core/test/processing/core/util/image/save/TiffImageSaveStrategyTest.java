package processing.core.util.image.save;

import org.junit.Test;
import processing.core.PConstants;
import processing.core.util.common.CommonInputTestUtil;

import java.io.IOException;


public class TiffImageSaveStrategyTest {

  @Test
  public void testSave() throws IOException {
    TiffImageSaveStrategy strategy = new TiffImageSaveStrategy();
    int[] pixels = CommonInputTestUtil.generateTestPixels();

    strategy.save(
        pixels,
        7,
        5,
        PConstants.RGB,
        CommonInputTestUtil.getFullPath(
            "resource-test/scratch/test-image-writer.tiff"
        )
    );

    CommonInputTestUtil.checkSavedFile("resource-test/scratch/test-image-writer.tiff");
  }

}
