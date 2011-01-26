Spot sp1, sp2, sp3; // Declare the objects

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  sp1 = new Spot(20, 50, 40, 0.5); // Construct sp1
  sp2 = new Spot(50, 50, 10, 2.0); // Construct sp2
  sp3 = new Spot(80, 50, 30, 1.5); // Construct sp3
}

void draw() {
  fill(0, 15);
  rect(0, 0, width, height);
  fill(255);
  sp1.move();
  sp2.move();
  sp3.move();
  sp1.display();
  sp2.display();
  sp3.display();
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

