// Spot
// by REAS <http://reas.com>

// Move the mouse the change the position and concentation
// of a blue spot light.

// Created 28 April 2005

int concentration = 600; // Try 1 -> 10000

void setup() 
{
  size(200, 200, P3D);
  noStroke();
  fill(204);
  sphereDetail(60);
}

void draw() 
{
  background(0); 
  directionalLight(51, 102, 126, 0, -1, 0);
  spotLight(204, 153, 0, 120, 80, 400, 0, 0, -1, PI/2, 600); 
  spotLight(102, 153, 204, 120, mouseY, 400, 0, 0, -1, PI/2, (mouseX * 90) + 180); 
  translate(160, 100, 0); 
  sphere(90); 
}

