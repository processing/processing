package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.image.ImageLoadFacade;

import javax.imageio.ImageIO;
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
          return ImageLoadFacade.get().loadImageIO(pApplet, path);
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

}
