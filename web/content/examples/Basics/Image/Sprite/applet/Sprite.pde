/**
 * Sprite (Teddy)
 * by James Patterson. 
 * 
 * Demonstrates loading and displaying a transparent GIF image. 
 */
 
PImage teddy;

float xpos;
float ypos;
float drag = 30.0;

void setup() 
{
  size(200,200);
  teddy = loadImage("teddy.gif");
  xpos = width/2;
  ypos = height/2;
  frameRate(60);
}

void draw() 
{ 
  background(102);
  
  float difx = mouseX - xpos-teddy.width/2;
  if(abs(difx) > 1.0) {
    xpos = xpos + difx/drag;
    xpos = constrain(xpos, 0, width-teddy.width);
  }  
  
  float dify = mouseY - ypos-teddy.height/2;
  if(abs(dify) > 1.0) {
    ypos = ypos + dify/drag;
    ypos = constrain(ypos, 0, height-teddy.height);
  }  
  
  // Display the sprite at the position xpos, ypos
  image(teddy, xpos, ypos);
}
