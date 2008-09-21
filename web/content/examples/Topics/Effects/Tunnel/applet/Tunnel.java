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

public class Tunnel extends PApplet {

/**
 * Tunnel Demo Effect 
 * by luis2048. 
 * 
 * This effect shows a tunnel in which you fly while the tunnel 
 * rotates, seemingly in 3D. The animation of the tunnel actually 
 * isn't calculated on the fly while the animation runs, but is 
 * precalculated. These calculations are stored in two tables: 
 * one for the angle and one for the distance. For every frame, 
 * go through every pixel (x,y) and use the angle and distance 
 * tables to get which pixel of the texture it should draw at the 
 * current pixel. To look like its rotating and zooming, the values 
 * of the angle and distance tables are shifted. 
 */

int x, y, radius, l;
PGraphics tunnelEffect;
PImage textureImg;

// build lookup table
int[][] distanceTable;
int[][] angleTable;
int[][] shadeTable;
int w, h;

public void setup(){
  size(640, 360);
  
  // Load texture 512 x 512
  textureImg=loadImage("red_smoke.jpg");

  // Create buffer screen
  tunnelEffect = createGraphics(320, 200, P2D);
  w = tunnelEffect.width;
  h = tunnelEffect.height;

  float ratio = 32.0f;
  int angle;
  int depth;
  int shade = 0;

  // Make the tables twice as big as the screen. 
  // The center of the buffers is now the position (w,h).
  distanceTable= new int[2 * w][2 * h];
  angleTable= new int[2 * w][2 * h];

  for (int x = 0; x < w*2; x++)
  {
    for (int y = 0; y < h*2; y++)
    {
      depth = PApplet.parseInt(ratio * textureImg.height 
                  / sqrt(PApplet.parseFloat((x - w) * (x - w) + (y - h) * (y - h)))) ;
      angle = PApplet.parseInt(0.5f * textureImg.width * atan2(PApplet.parseFloat(y - h), 
                  PApplet.parseFloat(x - w)) / PI) ;

      // The distance table contains for every pixel of the 
      // screen, the inverse of the distance to the center of 
      // the screen this pixel has.
      distanceTable[x][y] = depth ;

      // The angle table contains the angle of every pixel of the screen, 
      // where the center of the screen represents the origin.
      angleTable[x][y] = angle ;
    }
  }   
}


public void draw() {

  tunnelEffect.beginDraw();
  tunnelEffect.loadPixels();

  float timeDisplacement = millis() / 1000.0f;

  // Calculate the shift values out of the time value
  int shiftX = PApplet.parseInt(textureImg.width * .2f * timeDisplacement+300); // speed of zoom
  int shiftY = PApplet.parseInt(textureImg.height * .15f * timeDisplacement+300); //speed of spin

  // Calculate the look values out of the time value
  // by using sine functions, it'll alternate between 
  // looking left/right and up/down
  int shiftLookX = w / 2 + PApplet.parseInt(w / 4 * sin(timeDisplacement));
  int shiftLookY = h / 2 + PApplet.parseInt(h / 4 * sin(timeDisplacement * 1.5f));

  for (int y = 0; y < h; y++)  {
    for (int x = 0; x < w; x++)      {
      
      // Make sure that x + shiftLookX never goes outside 
      // the dimensions of the table
      int texture_x = constrain((distanceTable[x + shiftLookX][y + shiftLookY] 
                                 + shiftX) % textureImg.width ,0, textureImg.width);
      
      int texture_y = (angleTable[x + shiftLookX][y + shiftLookY] 
                       + shiftY) % textureImg.height;
      
      tunnelEffect.pixels[x+y*w] = textureImg.pixels[texture_y 
                         * textureImg.width + texture_x];

      // Test lookuptables
      // tunnelEffect.pixels[x+y*w] = color( 0,texture_x,texture_y);
    }
  }

  tunnelEffect.updatePixels();
  tunnelEffect.endDraw();

  // Display the results
  image(tunnelEffect, 0, 0, width, height); 

}



  static public void main(String args[]) {
    PApplet.main(new String[] { "Tunnel" });
  }
}
