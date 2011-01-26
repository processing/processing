Spot sp; // Declare the object

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  sp = new Spot(); // Construct the object
  sp.x = 33;
  sp.y = 50;
  sp.diameter = 30;
}

void draw() {
  background(0);
  sp.display();
}

class Spot {
  float x, y, diameter;
  void display() {
    ellipse(x, y, diameter, diameter);
  }
}
