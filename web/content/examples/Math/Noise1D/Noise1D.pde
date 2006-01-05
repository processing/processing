// Noise1D
// Daniel Shiffman <http://www.shiffman.net>

// Using 1D Perlin Noise to assign location

// Created 2 May 2005

float xoff = 0.0;
float xincrement = 0.01; 

void setup() {
  size(200,200);
  background(0);
  framerate(30);
  smooth();
  noStroke();
}

void draw()
{
  // Create an alpha blended background
  fill(0, 10);
  rect(0,0,width,height);
  
  //float n = random(0,width);  // Try this line instead of noise
  
  // Get a noise value based on xoff and scale it according to the window's width
  float n = noise(xoff)*width;
  
  // With each cycle, increment xoff
  xoff += xincrement;
  
  // Draw the ellipse at the value produced by perlin noise
  fill(200);
  ellipse(n,height/2,16,16);
}


