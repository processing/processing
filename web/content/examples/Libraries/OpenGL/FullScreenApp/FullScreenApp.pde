/**
 * Full Screen Application. 
 * 
 * The main() function needs to be added for the program to run full screen
 * after it is exported as an application. Select "Export Application" from the file
 * menu to build an app for Mac OS X, Windows, and Linux.
 * 
 * The screen.width and screen.height variables into size() ensure it will always be 
 * the width and height of the screen. Be certain the 2nd element in the array inside 
 * main() is identical to the name of the sketch. By default, pressing the Esc key will 
 * quit the application.
 */


import processing.opengl.*;

static public void main(String args[]) {
  PApplet.main(new String[] { "--present", "FullScreenApp" });
}

void setup() 
{
  size(screen.width, screen.height, OPENGL);
  noStroke();
}

void draw() 
{
  lights();
  background(0);
  
  for (int x = 0; x <= width; x += 100) {
    for (int y = 0; y <= height; y += 100) {
      pushMatrix();
      translate(x, y);
      rotateY(map(mouseX, 0, width, 0, PI));
      rotateX(map(mouseY, 0, height, 0, PI));
      box(90);
      popMatrix();
    }
  }
}
