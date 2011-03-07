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

public class UnlimitedSprites extends PApplet {

/**
 * Unlimited Sprites Demo Effect
 * by luis2048.  
 * 
 * An infinate number of sprites drawn to screen. It's  basically 
 * a flick-book effect; you draw the same sprite in different 
 * positions on different bufffer 'screens' and flip between them.
 * When you've drawn on all frames, you loop back to the beginning 
 * and repeat.
 */

PGraphics[] spriteFrames = new PGraphics[6];
PImage sprite;

float x, y;
float xang = 0.0f;
float yang = 0.0f;
int surf = 0;

public void setup() {
  size(640, 360);
  noSmooth();
  background(0);
  
  // Create sprite
  sprite=loadImage("Aqua-Ball-48x48.png");

  // Create blank surfaces to draw on
  for (int i = 0; i < spriteFrames.length; i++)  {
    spriteFrames[i] = createGraphics(width, height, JAVA2D);
  }   
}

public void draw()
{
  background(0);
  
  // Get X, Y positions
  x = (width/2)*sin((radians(xang))*0.95f);
  y = (height/2)*cos((radians(yang))*0.97f);

  // Inc the angle of the sine
  xang += 1.17f;
  yang += 1.39f;

  // Blit our 'bob' on the 'active' surface
  spriteFrames[surf].beginDraw();
  spriteFrames[surf].image(sprite, x+(width/2)-32, y+(height/2)-32);
  spriteFrames[surf].endDraw();            

  // Blit the active surface to the screen
  image(spriteFrames[surf], 0, 0, width, height);

  // Inc the active surface number
  surf = (surf+1) % spriteFrames.length;

  // Display the results
  //image(spriteEffect, 0, 0, width, height); 
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "UnlimitedSprites" });
  }
}
