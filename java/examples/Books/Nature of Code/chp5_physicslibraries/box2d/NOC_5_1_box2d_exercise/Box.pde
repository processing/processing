// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A rectangular box
class Box  {

  float x,y;
  float w,h;

  // Constructor
  Box(float x_, float y_) {
    x = x_;
    y = y_;
    w = 16;
    h = 16;
  }

  // Drawing the box
  void display() {
    fill(127);
    stroke(0);
    strokeWeight(2);
    rectMode(CENTER);
    rect(x,y,w,h);
  }
}
