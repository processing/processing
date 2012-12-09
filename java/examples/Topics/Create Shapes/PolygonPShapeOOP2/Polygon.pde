// A class to describe a Polygon (with a PShape)

class Polygon {
  // The PShape object
  PShape s;
  // The location where we will draw the shape
  float x, y;
  // Variable for simple motion
  float speed;

  Polygon(PShape s_) {
    x = random(width);
    y = random(-500, -100); 
    s = s_;
    speed = random(2, 6);
  }
  
  // Simple motion
  void move() {
    y += speed;
    if (y > height+100) {
      y = -100;
    }
  }
  
  // Draw the object
  void display() {
    pushMatrix();
    translate(x, y);
    shape(s);
    popMatrix();
  }
}

