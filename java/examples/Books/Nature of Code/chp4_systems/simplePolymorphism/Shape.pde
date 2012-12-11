// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Shape {
  float x;
  float y;
  float r;
  
  Shape(float x_, float y_, float r_) {
    x = x_;
    y = y_;
    r = r_;
  }
  
  void jiggle() {
    x += random(-1,1);
    y += random(-1,1);
  }
  
  // A generic shape does not really know how to be displayed. 
  // This will be overridden in the child classes.
  void display() {
    point(x,y); 
  }
}
