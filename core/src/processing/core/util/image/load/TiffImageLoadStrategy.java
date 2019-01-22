package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.util.image.constants.TifConstants;


public class TiffImageLoadStrategy implements ImageLoadStrategy {

  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    byte bytes[] = pApplet.loadBytes(path);
    PImage image =  (bytes == null) ? null : loadFromBytes(bytes);
    return image;
  }

  private PImage loadFromBytes(byte[] tiff) {
    if ((tiff[42] != tiff[102]) ||  // width/height in both places
        (tiff[43] != tiff[103])) {
      System.err.println(TifConstants.TIFF_ERROR);
      return null;
    }

    int width =
        ((tiff[30] & 0xff) << 8) | (tiff[31] & 0xff);
    int height =
        ((tiff[42] & 0xff) << 8) | (tiff[43] & 0xff);

    int count =
        ((tiff[114] & 0xff) << 24) |
            ((tiff[115] & 0xff) << 16) |
            ((tiff[116] & 0xff) << 8) |
            (tiff[117] & 0xff);
    if (count != width * height * 3) {
      System.err.println(TifConstants.TIFF_ERROR + " (" + width + ", " + height +")");
      return null;
    }

    // check the rest of the header
    for (int i = 0; i < TifConstants.TIFF_HEADER.length; i++) {
      if ((i == 30) || (i == 31) || (i == 42) || (i == 43) ||
          (i == 102) || (i == 103) ||
          (i == 114) || (i == 115) || (i == 116) || (i == 117)) continue;

      if (tiff[i] != TifConstants.TIFF_HEADER[i]) {
        System.err.println(TifConstants.TIFF_ERROR + " (" + i + ")");
        return null;
      }
    }

    PImage outgoing = new PImage(width, height, PConstants.RGB);
    int index = 768;
    count /= 3;
    for (int i = 0; i < count; i++) {
      outgoing.pixels[i] =
          0xFF000000 |
              (tiff[index++] & 0xff) << 16 |
              (tiff[index++] & 0xff) << 8 |
              (tiff[index++] & 0xff);
    }
    return outgoing;
  }

}
