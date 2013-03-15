// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A list for all of our rectangles
ArrayList<Box> boxes;

void setup() {
  size(640,360);
  // Create ArrayLists
  boxes = new ArrayList<Box>();
}

void draw() {
  background(255);

  // When the mouse is clicked, add a new Box object
  if (mousePressed) {
    Box p = new Box(mouseX,mouseY);
    boxes.add(p);
  }

  // Display all the boxes
  for (Box b: boxes) {
    b.display();
  }
}
