// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Circle extends Shape {
  
  // Inherits all instance variables from parent + adding one
  color c;
  
  Circle(float x_, float y_, float r_, color c_) {
    super(x_,y_,r_); // Call the parent constructor
    c = c_;          // Also deal with this new instance variable
  }
  
  // Call the parent jiggle, but do some more stuff too
  void jiggle() {
    super.jiggle();
    // The Circle jiggles its size as well as its x,y location.
    r += random(-1,1); 
    r = constrain(r,0,100);
  }
  
  // The changeColor() function is unique to the Circle class.
  void changeColor() { 
    c = color(random(255));
  }
  
  void display() {
    ellipseMode(CENTER);
    fill(c);
    stroke(0);
    ellipse(x,y,r,r);
  }
}
