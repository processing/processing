// Translate 3D
// by REAS <http://reas.com>

// The third parameter to the translate() function
// sets the offset in the z-axis. Positive values
// move shapes toward the viewer and negative values 
// move shapes away from the viewer.

// Created 16 January 2003

float x, y;
float size = 40.0;

void setup() 
{
  size(200, 200, P3D);
  noStroke();
  framerate(30);
}

void draw() 
{
  background(102);
  
  x = x + 1.0;
  
  if (x > width) {
    x = -size;
  } 
  
  translate(x, height/2-size/2, x);
  fill(255);
  rect(-size/2, -size/2, size, size);
  
  translate(x, size, x/2);
  fill(0);
  rect(-size/2, -size/2, size, size);
}
