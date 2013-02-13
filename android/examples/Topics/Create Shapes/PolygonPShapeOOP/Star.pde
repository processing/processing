// A class to describe a Star shape

class Star {

  // The PShape object
  PShape s;
  // The location where we will draw the shape
  float x, y;
  float speed;

  Star() {
    x = random(100, width-100);
    y = random(100, height-100); 
    speed = random(0.5, 3);
    // First create the shape
    s = createShape();
    s.beginShape();
    // You can set fill and stroke
    s.fill(255, 204);
    s.noStroke();
    // Here, we are hardcoding a series of vertices
    s.vertex(0, -50);
    s.vertex(14, -20);
    s.vertex(47, -15);
    s.vertex(23, 7);
    s.vertex(29, 40);
    s.vertex(0, 25);
    s.vertex(-29, 40);
    s.vertex(-23, 7);
    s.vertex(-47, -15);
    s.vertex(-14, -20);
    // The shape is complete
    s.endShape(CLOSE);
  }

  void move() {
    // Demonstrating some simple motion
    x += speed;
    if (x > width+100) {
      x = -100;
    }
  }

  void display() {
    // Locating and drawing the shape
    pushMatrix();
    translate(x, y);
    shape(s);
    popMatrix();
  }
}

