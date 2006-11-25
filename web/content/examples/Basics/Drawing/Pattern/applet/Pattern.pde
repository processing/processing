// Patterns
// by REAS <http://reas.com>

// Move the cursor over the image to draw with a software tool 
// which responds to the speed of the mouse.

// Created 19 January 2003

int maxSpeed;
void setup()
{
  size(200, 200);
  background(102);
  
  // Draw ellipses with their center specified by the first two 
  // paramenters to the ellipse() method
  ellipseMode(CENTER);
}

void draw() 
{
  // Call the variableEllipse() method and send it the
  // parameters for the current mouse position
  // and the previous mouse position
  variableEllipse(mouseX, mouseY, pmouseX, pmouseY);
}


// The simple method variableEllipse() was created specifically 
// for this program. It calculates the speed of the mouse
// and draws a small ellipse if the mouse is moving slowly
// and draws a large ellipse if the mouse is moving quickly 

void variableEllipse(int x, int y, int px, int py) 
{
  float speed = abs(x-px) + abs(y-py);
  stroke(speed);
  ellipse(x, y, speed, speed);
}
