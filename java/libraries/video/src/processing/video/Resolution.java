package processing.video;

import processing.core.PApplet;

/**
 * Utility class to store the resolution (width, height and fps) of a capture
 * device.
 * 
 */
public class Resolution {
  public int width, height;
  public float fps;
  public String fpsString;
  
  public Resolution() {
    width = height = 0;
    fps = 0.0f;
    fpsString = "";
  }
  
  public Resolution(int width, int height, int fpsDenominator, int fpsNumerator) {
    this.width = width;
    this.height = height;
    this.fps = (float)fpsDenominator / (float)fpsNumerator;
    this.fpsString = fpsDenominator + "/" + fpsNumerator;
  }
  
  public Resolution(int width, int height, String fpsString) {
    this.width = width;
    this.height = height;
    
    String[] parts = fpsString.split("/");
    if (parts.length == 2) {      
      int fpsDenominator = PApplet.parseInt(parts[0]);
      int fpsNumerator = PApplet.parseInt(parts[1]);
    
      this.fps = (float)fpsDenominator / (float)fpsNumerator;
      this.fpsString = fpsString;
    } else {
      this.fps = 0.0f;
      this.fpsString = "";
    }
  }
  
  public Resolution(Resolution source) {
    this.width = source.width;
    this.height = source.height;
    this.fps = source.fps;
    this.fpsString = source.fpsString;
  }  
  
  public String toString() {
    return width + "x" + height + ", " + PApplet.nfc(fps, 2) + "fps (" + fpsString +")";    
  }
}