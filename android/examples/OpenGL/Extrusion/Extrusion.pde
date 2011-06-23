// Extrusion. 
// Converts a flat image into spatial data points and rotates the points
// around the center.
//
// PERFORMANCE ON ANDROID of this example is very low because 
// line drawing in A3D needs to be reimplemented. See this bug 
// report: http://code.google.com/p/processing/issues/detail?id=123

PImage a;
boolean onetime = true;
int[][] aPixels;
int[][] values;
float angle;

void setup() {
  size(480, 800, P3D);
  orientation(PORTRAIT);
  
  aPixels = new int[width][height];
  values = new int[width][height];
  noFill();

  // Load the image into a new array
  // Extract the values and store in an array
  a = loadImage("ystone08.jpg");
  a.loadPixels();
  for (int i = 0; i < a.height; i++) {
    for (int j = 0; j < a.width; j++) {
      aPixels[j][i] = a.pixels[i*a.width + j];
      values[j][i] = int(blue(aPixels[j][i]));
    }
  }
}

void draw() {
  background(255);
  translate(width/2, height/2, 0);
  scale(2.0);
  
  // Update and constrain the angle
  angle += 0.05;
  rotateY(angle);  
  
  // Display the image mass
  for (int i = 0; i < a.height; i += 2) {
    for (int j = 0; j < a.width; j += 2) {
      stroke(values[j][i], 153);
      line(j-a.width/2, i-a.height/2, -values[j][i], j-a.width/2, i-a.height/2, -values[j][i]-10);
    }
  }
}
