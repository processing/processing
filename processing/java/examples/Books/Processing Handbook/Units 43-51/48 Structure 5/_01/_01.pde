Spot sp1, sp2;

void setup() {
  size(100, 100);
  smooth();
  noLoop();
// Run the constructore without parameters
  sp1 = new Spot();
// Run the constructor with three parameters
  sp2 = new Spot(66, 50, 20);
}

void draw() {
  sp1.display();
  sp2.display();
}

class Spot {
  float x, y, radius;
// First version of the Spot constructor,
// the fields are assigned default values
  Spot() {
    x = 33;
    y = 50;
    radius = 8;
  }
// Second version of the Spot constructor,
// the fields are assigned with parameters
  Spot(float xpos, float ypos, float r) {
    x = xpos;
    y = ypos;
    radius = r;
  }
  void display() {
    ellipse(x, y, radius*2, radius*2);
  }
}
