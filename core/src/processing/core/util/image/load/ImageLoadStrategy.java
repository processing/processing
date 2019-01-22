package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PImage;


public interface ImageLoadStrategy {

  PImage load(PApplet pApplet, String path, String extension);

}
