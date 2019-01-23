package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.image.ImageLoadFacade;
import processing.core.util.io.InputFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Use Java 1.4 ImageIO methods to load an image.
 */
public class ImageIoImageLoadStrategy implements ImageLoadStrategy {

  private AtomicReference<String[]> cachedLoadImageFormats;

  public ImageIoImageLoadStrategy() {
    cachedLoadImageFormats = new AtomicReference<>(null);
  }

  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    String[] loadImageFormats = getLoadImageFormats();

    if (loadImageFormats != null) {
      for (int i = 0; i < loadImageFormats.length; i++) {
        if (extension.equals(loadImageFormats[i])) {
          return loadImageIOInner(pApplet, path);
        }
      }
    }

    // failed, could not load image after all those attempts
    System.err.println("Could not find a method to load " + path);
    return null;
  }

  private String[] getLoadImageFormats() {
    cachedLoadImageFormats.compareAndSet(null, ImageIO.getReaderFormatNames());
    return cachedLoadImageFormats.get();
  }

  public PImage loadImageIOInner(PApplet pApplet, String filename) {
    InputStream stream = InputFactory.createInput(pApplet, filename);
    if (stream == null) {
      System.err.println("The image " + filename + " could not be found.");
      return null;
    }

    try {
      BufferedImage bi = ImageIO.read(stream);

      int width = bi.getWidth();
      int height = bi.getHeight();
      int[] pixels = new int[width * height];

      bi.getRGB(0, 0, width, height, pixels, 0, width);

      // check the alpha for this image
      // was gonna call getType() on the image to see if RGB or ARGB,
      // but it's not actually useful, since gif images will come through
      // as TYPE_BYTE_INDEXED, which means it'll still have to check for
      // the transparency. also, would have to iterate through all the other
      // types and guess whether alpha was in there, so.. just gonna stick
      // with the old method.
      PImage outgoing = new PImage(width, height, pixels, true, pApplet);

      stream.close();
      // return the image
      return outgoing;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
