// Learning Processing
// Daniel Shiffman
// http://www.learningprocessing.com

// Example 22-1: Inheritance

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
