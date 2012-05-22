/**
 * PolygonPShapeOOP. 
 * 
 * Wrapping a PShape inside a custom class 
 * and demonstrating how we can have a multiple objects each
 * using the same PShape.
 */


// A list of objects
ArrayList<Polygon> polygons;

void setup() {
  size(640, 360, P3D);
  smooth();
  // Make a PShape
  PShape star = createShape(POLYGON);
  star.fill(0,127);
  star.stroke(0);
  star.vertex(0, -50);
  star.vertex(14, -20);
  star.vertex(47, -15);
  star.vertex(23, 7);
  star.vertex(29, 40);
  star.vertex(0, 25);
  star.vertex(-29, 40);
  star.vertex(-23, 7);
  star.vertex(-47, -15);
  star.vertex(-14, -20);
  star.end(CLOSE);
  
  // Make an ArrayList
  polygons = new ArrayList<Polygon>();
  
  // Add a bunch of objects to the ArrayList
  // Pass in reference to the PShape
  // We coud make polygons with different PShapes
  for (int i = 0; i < 25; i++) {
    polygons.add(new Polygon(star));
  }
}

void draw() {
  background(255);

  // Display and move them all
  for (Polygon poly : polygons) {
    poly.display();
    poly.move();
  }
}

