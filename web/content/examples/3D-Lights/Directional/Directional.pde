// Directional
// by REAS <http://reas.com>

// Move the mouse the change the direction of the light.
// Directional light comes from one direction and is stronger 
// when hitting a surface squarely and weaker if it hits at a 
// a gentle angle. After hitting a surface, a directional lights 
// scatters in all directions.

// Created 28 April 2005


void setup() 
{
  size(200, 200, P3D);
  noStroke();
  fill(204);
}

void draw() 
{
  noStroke(); 
  background(0); 
  float dirY = (mouseY/float(height) - 0.5) * 2.0;
  float dirX = (mouseX/float(width) - 0.5) * 2.0;
  directionalLight(204, 204, 204, -dirX, -dirY, -1); 
  translate(20, height/2, 0); 
  sphere(60); 
  translate(120, 0, 0); 
  sphere(60); 
}

