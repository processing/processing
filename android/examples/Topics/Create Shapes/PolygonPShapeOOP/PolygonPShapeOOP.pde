/**
 * PolygonPShapeOOP. 
 * 
 * Wrapping a PShape inside a custom class 
 */


// A Star object
Star s;

void setup() {
  size(640, 360, P2D);
  orientation(LANDSCAPE);
  // Make a new Star
  s = new Star();

}

void draw() {
  background(51);
  // Display the star
  s.display();
  // Move the star
  s.move();
}

