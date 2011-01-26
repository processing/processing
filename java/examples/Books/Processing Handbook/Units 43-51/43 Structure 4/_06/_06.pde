Spot sp; // Declare the object

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  sp = new Spot(33, 50, 30, 1.5); // Construct the object
}

void draw() {
  fill(0, 15);
  rect(0, 0, width, height);
  fill(255);
  sp.move();
  sp.display();
}

class Spot {
  float x, y; // X-coordinate, y-coordinate
  float diameter; // Diameter of the circle
  float speed; // Distance moved each frame
  int direction = 1; // Direction of motion (1 is down, -1 is up)

  // Constructor
  Spot(float xpos, float ypos, float dia, float sp) {
    x = xpos;
    y = ypos;
    diameter = dia;
    speed = sp;
  }

  void move() {
    y += (speed * direction);
    if ((y > (height - diameter / 2)) || (y < diameter / 2)) {
      direction *= -1;
    }
  }

  void display() {
    ellipse(x, y, diameter, diameter);
  }
}
