/**
 * Extrusion. 
 * 
 * Converts a flat image into spatial data points and rotates the points
 * around the center.
 * 
 * Created 18 August 2002
 */

PImage extrude;
int[][] values;
float angle;

void setup() {
  size(200, 200, P3D);
  
  // Load the image into a new array
  extrude = loadImage("ystone08.jpg");
  extrude.loadPixels();
  values = new int[extrude.width][extrude.height];
  for (int y = 0; y < extrude.height; y++) {
    for (int x = 0; x < extrude.width; x++) {
      int pixel = extrude.pixels[y*width + x];
      values[x][y] = int(brightness(pixel));
    }
  }
}


void draw() {
  background(0);
  
  // Update and constrain the angle
  angle += 0.005;
  if (angle > TWO_PI) { angle = 0; }
  
  // Rotate around the center axis
  translate(width/2, 0, 128);
  rotateY(angle);  
  translate(-width/2, 0, 128);
  
  // Display the image mass
  for (int y = 0; y < extrude.height; y++) {
    for (int x = 0; x < extrude.width; x++) {
      stroke(values[x][y]);
      point(x, y, -values[x][y]);
    }
  }
}
