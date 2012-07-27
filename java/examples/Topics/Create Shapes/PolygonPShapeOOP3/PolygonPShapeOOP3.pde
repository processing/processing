/**
 * PolygonPShapeOOP. 
 * 
 * Wrapping a PShape inside a custom class 
 * and demonstrating how we can have a multiple objects each
 * using the same PShape.
 */


// A list of objects
ArrayList<Polygon> polygons;

// Three possible shapes
PShape[] shapes = new PShape[3];

void setup() {
  size(640, 360, P2D);
  smooth();
  
  shapes[0] = createShape(ELLIPSE,0,0,100,100);
  shapes[0].fill(255,127);
  shapes[0].stroke(0);
  shapes[1] = createShape(RECT,0,0,100,100);
  shapes[1].fill(255,127);
  shapes[1].stroke(0);
  shapes[2] = createShape();
  shapes[2].fill(0,127);
  shapes[2].stroke(0);
  shapes[2].vertex(0, -50);
  shapes[2].vertex(14, -20);
  shapes[2].vertex(47, -15);
  shapes[2].vertex(23, 7);
  shapes[2].vertex(29, 40);
  shapes[2].vertex(0, 25);
  shapes[2].vertex(-29, 40);
  shapes[2].vertex(-23, 7);
  shapes[2].vertex(-47, -15);
  shapes[2].vertex(-14, -20);
  shapes[2].end(CLOSE);


  // Make an ArrayList
  polygons = new ArrayList<Polygon>();
  
  polygons = new ArrayList<Polygon>();
  
  for (int i = 0; i < 25; i++) {
    int selection = int(random(shapes.length));        // Pick a random index
    Polygon p = new Polygon(shapes[selection]);        // Use corresponding PShape to create Polygon
    polygons.add(p);
  }
}

void draw() {
  background(51);

  // Display and move them all
  for (Polygon poly : polygons) {
    poly.display();
    poly.move();
  }
}

