// A list for all of our rectangles
ArrayList<Box> boxes;

void setup() {
  size(800,200);
  smooth();
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
