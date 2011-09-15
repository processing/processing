/**
 * Sprite (Teddy)
 * by James Patterson. 
 * 
 * Demonstrates loading and displaying a transparent GIF image. 
 */
 
PImage teddy;

float x, y;
float easing = 0.05;

void setup() {
  size(640, 360);
  imageMode(CENTER);
  teddy = loadImage("teddy.gif");
  x = width/2;
  y = height/2;
}

void draw() { 
  background(102);
  
  float dx = mouseX - x;
  x += dx * easing;
  x = constrain(x, teddy.width/2, width - teddy.width/2); 
  
  float dy = mouseY - y;
  y += dy * easing;
  y = constrain(y, teddy.height/2, height - teddy.height/2); 
  
  // Display the sprite at the position x, y
  image(teddy, x, y);
}
