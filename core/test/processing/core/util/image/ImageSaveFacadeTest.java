package processing.core.util.image;

import org.junit.Before;
import org.junit.Test;
import processing.core.PConstants;
import processing.core.util.common.CommonInputTestUtil;


public class ImageSaveFacadeTest {

  private ImageSaveFacade imageSaveFacade;
  private int[] pixels;

  @Before
  public void setUp() {
    imageSaveFacade = ImageSaveFacade.get();
    pixels = CommonInputTestUtil.generateTestPixels();
  }

  @Test
  public void testPngWrite() {
    imageSaveFacade.save(
        pixels,
        7,
        5,
        PConstants.RGB,
        CommonInputTestUtil.getFullPath(
            "resource-test/scratch/test-facade.png"
        )
    );

    CommonInputTestUtil.checkSavedFile("resource-test/scratch/test-facade.png");
  }

  @Test
  public void testTiffWrite() {
    imageSaveFacade.save(
        pixels,
        7,
        5,
        PConstants.RGB,
        CommonInputTestUtil.getFullPath(
            "resource-test/scratch/test-facade.tiff"
        )
    );

    CommonInputTestUtil.checkSavedFile("resource-test/scratch/test-facade.tiff");
  }

  @Test
  public void testTgaWrite() {
    imageSaveFacade.save(
        pixels,
        7,
        5,
        PConstants.RGB,
        CommonInputTestUtil.getFullPath(
            "resource-test/scratch/test-facade.tga"
        )
    );

    CommonInputTestUtil.checkSavedFile("resource-test/scratch/test-facade.tga");
  }

}
