Spot sp; // Declare the object

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  sp = new Spot(33, 50, 30); // Construct the object
}

void draw() {
  background(0);
  sp.display();
}

class Spot {
  float x, y, diameter;

  Spot(float xpos, float ypos, float dia) {
    x = xpos; // Assign 33 to x
    y = ypos; // Assign 50 to y
    diameter = dia; // Assign 30 to diameter
  }

  void display() {
    ellipse(x, y, diameter, diameter);
  }
}
