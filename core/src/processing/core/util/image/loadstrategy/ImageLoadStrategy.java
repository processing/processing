package processing.core.util.image.loadstrategy;

import processing.core.PApplet;
import processing.core.PImage;


public interface ImageLoadStrategy {

  PImage load(PApplet pApplet, String path, String extension);

}
