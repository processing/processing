int numSpots = 6;
// Declare and create the array
Spot[] spots = new Spot[numSpots];

void setup() {
  size(100, 100);
  smooth();
  noStroke();
  for (int i = 0; i < spots.length; i++) {
    float x = 10 + i * 16;
    float rate = 0.5 + i * 0.05;
// Create each object
    spots[i] = new Spot(x, 50, 16, rate);
  }
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  for (int i = 0; i < spots.length; i++) {
    spots[i].move(); // Move each object
    spots[i].display(); // Display each object
  }
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


