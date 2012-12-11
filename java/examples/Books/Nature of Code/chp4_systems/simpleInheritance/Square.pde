// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Square extends Shape {
  // Variables are inherited from the parent.
  // We could also add variables unique to the Square class if we so desire

  Square(float x_, float y_, float r_) {
    // If the parent constructor takes arguments then super() needs to pass in those arguments.
    super(x_,y_,r_); 
  }

  // Inherits jiggle() from parent

  // The square overrides its parent for display.
  void display() {
    rectMode(CENTER);
    fill(175);
    stroke(0);
    rect(x,y,r,r);
  }
}
