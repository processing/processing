package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PImage;

public class FallbackImageLoadStrategy implements ImageLoadStrategy {

  private ImageLoadStrategy primaryStrategy;
  private ImageLoadStrategy secondaryStrategy;

  public FallbackImageLoadStrategy(ImageLoadStrategy newPrimaryStrategy,
      ImageLoadStrategy newSecondaryStrategy) {

    primaryStrategy = newPrimaryStrategy;
    secondaryStrategy = newSecondaryStrategy;
  }

  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    try {
      return primaryStrategy.load(pApplet, path, extension);
    } catch (Exception e) {
      // show error, but move on to the stuff below, see if it'll work
      e.printStackTrace();

      return secondaryStrategy.load(pApplet, path, extension);
    }
  }

}
