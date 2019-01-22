package processing.core.util.image;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.util.image.save.*;
import processing.core.util.io.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ImageSaveFacade {

  private static final AtomicReference<ImageSaveFacade> instance = new AtomicReference<>(null);

  private final Map<String, ImageSaveStrategy> saveStrategies;
  private final ImageSaveStrategy defaultImageSaveStrategy;

  public static ImageSaveFacade get() {
    instance.compareAndSet(null, new ImageSaveFacade());
    return instance.get();
  }

  private ImageSaveFacade() {
    saveStrategies = new HashMap<>();

    ImageSaveStrategy imageWriterImageSaveStrategy = new ImageWriterImageSaveStrategy();
    for (String format : javax.imageio.ImageIO.getWriterFormatNames()) {
      saveStrategies.put(format.toLowerCase(), imageWriterImageSaveStrategy);
    }

    ImageSaveStrategy tgaImageSaveStrategy = new TgaImageSaveStrategy();
    saveStrategies.put("tga", tgaImageSaveStrategy);

    ImageSaveStrategy tiffImageSaveStrategy = new TiffImageSaveStrategy();
    saveStrategies.put("tiff", tiffImageSaveStrategy);

    defaultImageSaveStrategy = new TiffNakedFilenameImageSaveStrategy();
  }

  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format, String filename) {
    return save(
        pixels,
        pixelWidth,
        pixelHeight,
        format,
        filename,
        null
    );
  }

  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format, String filename,
      PApplet pApplet) {

    filename = preparePath(pApplet, filename);

    String extension = PathUtil.parseExtension(filename);

    ImageSaveStrategy imageSaveStrategy = saveStrategies.getOrDefault(
        extension,
        defaultImageSaveStrategy
    );

    try {
      return imageSaveStrategy.save(
          pixels,
          pixelWidth,
          pixelHeight,
          format,
          filename
      );
    } catch (IOException e) {
      System.err.println("Error while saving image.");
      e.printStackTrace();
      return false;
    } catch (SaveImageException e) {
      PGraphics.showException(e.getMessage());
      return false;
    }

  }

  private String preparePath(PApplet pApplet, String filename) {
    if (pApplet != null) {
      return pApplet.savePath(filename);
    } else {
      File file = new File(filename);
      if (file.isAbsolute()) {
        // make sure that the intermediate folders have been created
        PathUtil.createPath(file);
        return filename;
      } else {
        String msg =
            "PImage.save() requires an absolute path. " +
                "Use createImage(), or pass savePath() to save().";

        throw new SaveImageException(msg);
      }
    }
  }

}
