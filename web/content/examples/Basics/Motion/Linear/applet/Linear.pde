// Linear
// by REAS <http://reas.com>

// Changing a variable to create a moving line. 
// When the line moves off the edge of the window, 
// the variable is set to 0, which places the line
// back at the bottom of the screen.

// Updated 21 August 2002

float a = 100;

void setup() 
{
  size(200, 200);
  stroke(255);
  framerate(30);
}

void draw() 
{
  background(51);
  a = a - 1;
  if (a < 0) { 
    a = height; 
  }
  line(0, a, width, a);  
}
