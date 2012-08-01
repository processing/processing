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
float xang = 0.0;
float yang = 0.0;
int surf = 0;

void setup() {
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

void draw()
{
  background(0);
  
  // Get X, Y positions
  x = (width/2)*sin((radians(xang))*0.95);
  y = (height/2)*cos((radians(yang))*0.97);

  // Inc the angle of the sine
  xang += 1.17;
  yang += 1.39;

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

