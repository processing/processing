// A class to describe a Star shape

class Star {

  // The PShape object
  PShape s;
  // The location where we will draw the shape
  float x, y;

  Star() {
    x = 0;
    y = height/2; 
    // First create the shape
    s = createShape();
    // You can set fill and stroke
    s.fill(102);
    s.stroke(255);
    s.strokeWeight(2);
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
    s.end(CLOSE);
  }

  void move() {
    // Demonstrating some simple motion
    x++;
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

