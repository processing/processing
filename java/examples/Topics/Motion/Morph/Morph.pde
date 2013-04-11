/**
 * Morph. 
 * 
 * Changing one shape into another by interpolating
 * vertices from one to another
 */

// Two ArrayLists to store the vertices for two shapes
// This example assumes that each shape will have the same
// number of vertices, i.e. the size of each ArrayList will be the same
ArrayList<PVector> circle = new ArrayList<PVector>();
ArrayList<PVector> square = new ArrayList<PVector>();

// An ArrayList for a third set of vertices, the ones we will be drawing
// in the window
ArrayList<PVector> morph = new ArrayList<PVector>();

// This boolean variable will control if we are morphing to a circle or square
boolean state = false;

void setup() {
  size(640, 360);

  // Create a circle using vectors pointing from center
  for (int angle = 0; angle < 360; angle += 9) {
    // Note we are not starting from 0 in order to match the
    // path of a circle.  
    PVector v = PVector.fromAngle(radians(angle-135));
    v.mult(100);
    circle.add(v);
    // Let's fill out morph ArrayList with blank PVectors while we are at it
    morph.add(new PVector());
  }

  // A square is a bunch of vertices along straight lines
  // Top of square
  for (int x = -50; x < 50; x += 10) {
    square.add(new PVector(x, -50));
  }
  // Right side
  for (int y = -50; y < 50; y += 10) {
    square.add(new PVector(50, y));
  }
  // Bottom
  for (int x = 50; x > -50; x -= 10) {
    square.add(new PVector(x, 50));
  }
  // Left side
  for (int y = 50; y > -50; y -= 10) {
    square.add(new PVector(-50, y));
  }
}

void draw() {
  background(51);

  // We will keep how far the vertices are from their target
  float totalDistance = 0;
  
  // Look at each vertex
  for (int i = 0; i < circle.size(); i++) {
    PVector v1;
    // Are we lerping to the circle or square?
    if (state) {
      v1 = circle.get(i);
    }
    else {
      v1 = square.get(i);
    }
    // Get the vertex we will draw
    PVector v2 = morph.get(i);
    // Lerp to the target
    v2.lerp(v1, 0.1);
    // Check how far we are from target
    totalDistance += PVector.dist(v1, v2);
  }
  
  // If all the vertices are close, switch shape
  if (totalDistance < 0.1) {
    state = !state;
  }
  
  // Draw relative to center
  translate(width/2, height/2);
  strokeWeight(4);
  // Draw a polygon that makes up all the vertices
  beginShape();
  noFill();
  stroke(255);
  for (PVector v : morph) {
    vertex(v.x, v.y);
  }
  endShape(CLOSE);
}

