package processing.core.util.image;


import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.image.load.*;
import processing.core.util.io.InputFactory;
import processing.core.util.io.PathUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility functions for loading images either from file system or via string encoding.
 *
 * Utility functions for use within processing.core that provide high level image loading
 * functionality.
 */
public class ImageLoadFacade {

  private static final AtomicReference<ImageLoadFacade> instance = new AtomicReference<>(null);

  private static final String PREFIX_BASE64_STRING_IMAGE = "data:image";
  private static final String PREFIX_FILE_PATH = "file://";

  private final Map<String, ImageLoadStrategy> loadStrategies;
  private final ImageLoadStrategy defaultImageLoadStrategy;

  public static ImageLoadFacade get() {
    instance.compareAndSet(null, new ImageLoadFacade());
    return instance.get();
  }

  private ImageLoadFacade() {
    loadStrategies = new HashMap<>();

    loadStrategies.put("base64", new Base64StringImageLoadStrategy());

    loadStrategies.put("tga", new TgaImageLoadStrategy());

    ImageLoadStrategy tifImageLoadStrategy = new TiffImageLoadStrategy();
    loadStrategies.put("tif", tifImageLoadStrategy);
    loadStrategies.put("tiff", tifImageLoadStrategy);

    ImageLoadStrategy awtImageLoadStrategy = new AwtImageLoadStrategy();
    defaultImageLoadStrategy = new ImageIoImageLoadStrategy();
    ImageLoadStrategy imageIoWithFallbackStrategy = new FallbackImageLoadStrategy(
        awtImageLoadStrategy,
        defaultImageLoadStrategy
    );
    loadStrategies.put("jpg", imageIoWithFallbackStrategy);
    loadStrategies.put("jpeg", imageIoWithFallbackStrategy);
    loadStrategies.put("gif", imageIoWithFallbackStrategy);
    loadStrategies.put("png", imageIoWithFallbackStrategy);
    loadStrategies.put("unknown", imageIoWithFallbackStrategy);
  }

  public PImage loadImageIO(PApplet pApplet, String filename) {
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

  public PImage loadFromSvg(PApplet pApplet, String svgImageStr) {
    if (svgImageStr == null) {
      return null;
    }

    if (svgImageStr.startsWith(PREFIX_BASE64_STRING_IMAGE)) {
      String[] parts = svgImageStr.split(";base64,");
      String extension = parts[0].substring(11);
      String encodedData = parts[1];
      return loadStrategies.get("base64").load(pApplet, encodedData, extension);
    } else if (svgImageStr.startsWith(PREFIX_FILE_PATH)) {
      String filePath = svgImageStr.substring(PREFIX_FILE_PATH.length());
      return loadFromFile(pApplet, filePath);
    } else {
      return null;
    }
  }

  public PImage loadFromFile(PApplet pApplet, String path) {
    return loadFromFile(pApplet, path, null);
  }

  public PImage loadFromFile(PApplet pApplet, String path, String extension) {
    if (extension == null) {
      extension = PathUtil.parseExtension(path);
    }

    // just in case. them users will try anything!
    extension = PathUtil.cleanExtension(extension);

    // Find strategy for loading
    ImageLoadStrategy imageLoadStrategy = loadStrategies.getOrDefault(
        extension,
        defaultImageLoadStrategy
    );

    // Load image
    PImage resultImage = imageLoadStrategy.load(pApplet, path, extension);

    // Report error or return
    if (resultImage == null) {
      System.err.println("Could not find a method to load " + path);
      return null;
    } else {
      return resultImage;
    }
  }

}
