import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class Coordinates extends PApplet {
  public void setup() {/**
 * Coordinates. 
 * 
 * All shapes drawn to the screen have a position that is specified as a coordinate.
 * All coordinates are measured as the distance from the origin in units of pixels.
 * The origin [0, 0] is the coordinate is in the upper left of the window
 * and the coordinate in the lower right is [width-1, height-1].  
 */

// Sets the screen to be 200, 200, so the width of the window is 200 pixels
// and the height of the window is 200 pixels
size(200, 200);
background(0);
noFill();
stroke(255);

// The two parameters of the point() method each specify coordinates.
// This call to point() draws at the position [100, 100]
point(width/2, height/2);

// Draws to the position [100, 50]
point(width/2, height/4); 

// It is also possible to specify a point with any parameter, 
// but only coordinates on the screen are visible
point(60, 30);
point(60, 134);
point(160, 50);
point(280, -800);
point(201, 100);

// Coordinates are used for drawing all shapes, not just points.
// Parameters for different methods are used for different purposes.
// For example, the first two parameters to line() specify the coordinates of the 
// first point and the second two parameters specify the second point
stroke(204);
line(0, 73, width, 73);

// The first two parameters to rect() are coordinates
// and the second two are the width and height
rect(110, 55, 40, 36);

  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "Coordinates" });
  }
}
