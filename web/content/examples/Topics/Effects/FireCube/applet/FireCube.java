import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class FireCube extends PApplet {

/**
 * Fire Cube demo effect
 * by luis2048.
 * 
 * A rotating wireframe cube with flames rising up the screen.
 * The fire effect has been used quite often for oldskool demos.
 * First you create a palette of 256 colors ranging from red to 
 * yellow (including black). For every frame, calculate each row 
 * of pixels based on the two rows below it: The value of each pixel, 
 * becomes the sum of the 3 pixels below it (one directly below, one 
 * to the left, and one to the right), and one pixel directly two 
 * rows below it. Then divide the sum so that the fire dies out as 
 * it rises.
 */
 
// This will contain the pixels used to calculate the fire effect
int[][] fire;

// Flame colors
int[] palette;
float angle;
int[] calc1,calc2,calc3,calc4,calc5;

PGraphics pg;

public void setup(){
  size(640, 360, P3D);
  
  // Create buffered image for 3d cube
  pg = createGraphics(width, height, P3D);

  calc1 = new int[width];
  calc3 = new int[width];
  calc4 = new int[width];
  calc2 = new int[height];
  calc5 = new int[height];

  colorMode(HSB);

  fire = new int[width][height];
  palette = new int[255];

  // Generate the palette
  for(int x = 0; x < palette.length; x++) {
    //Hue goes from 0 to 85: red to yellow
    //Saturation is always the maximum: 255
    //Lightness is 0..255 for x=0..128, and 255 for x=128..255
    palette[x] = color(x/3, 255, constrain(x*3, 0, 255));
  }

  // Precalculate which pixel values to add during animation loop
  // this speeds up the effect by 10fps
  for (int x = 0; x < width; x++) {
    calc1[x] = x % width;
    calc3[x] = (x - 1 + width) % width;
    calc4[x] = (x + 1) % width;
  }
  
  for(int y = 0; y < height; y++) {
    calc2[y] = (y + 1) % height;
    calc5[y] = (y + 2) % height;
  }
}

public void draw() {
  angle = angle + 0.05f;

  // Rotating wireframe cube
  pg.beginDraw();
  pg.translate(width >> 1, height >> 1);
  pg.rotateX(sin(angle/2));
  pg.rotateY(cos(angle/2));
  pg.background(0);
  pg.stroke(128);
  pg.scale(25);
  pg.noFill();
  pg.box(4);
  pg.endDraw();

  // Randomize the bottom row of the fire buffer
  for(int x = 0; x < width; x++)
  {
    fire[x][height-1] = PApplet.parseInt(random(0,190)) ;
  }

  loadPixels();

  int counter = 0;
  // Do the fire calculations for every pixel, from top to bottom
  for (int y = 0; y < height; y++) {
    for(int x = 0; x < width; x++) {
      // Add pixel values around current pixel

      fire[x][y] =
          ((fire[calc3[x]][calc2[y]]
          + fire[calc1[x]][calc2[y]]
          + fire[calc4[x]][calc2[y]]
          + fire[calc1[x]][calc5[y]]) << 5) / 129;

      // Output everything to screen using our palette colors
      pixels[counter] = palette[fire[x][y]];

      // Extract the red value using right shift and bit mask 
      // equivalent of red(pg.pixels[x+y*w])
      if ((pg.pixels[counter++] >> 16 & 0xFF) == 128) {
        // Only map 3D cube 'lit' pixels onto fire array needed for next frame
        fire[x][y] = 128;
      }
    }
  }
  updatePixels();
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "FireCube" });
  }
}
