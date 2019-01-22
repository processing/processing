package processing.core.util.image.save;

import java.io.IOException;

public class TiffNakedFilenameImageSaveStrategy implements ImageSaveStrategy {

  private final ImageSaveStrategy tiffImageSaveStrategy;

  public TiffNakedFilenameImageSaveStrategy() {
    tiffImageSaveStrategy = new TiffImageSaveStrategy();
  }

  @Override
  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format,
      String filename) throws IOException {

    filename += ".tif";
    return tiffImageSaveStrategy.save(
        pixels,
        pixelWidth,
        pixelHeight,
        format,
        filename
    );
  }
}
