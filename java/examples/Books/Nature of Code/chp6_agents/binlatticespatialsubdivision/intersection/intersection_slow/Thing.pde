// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com
// Daniel Shiffman

// Simple class describing an ellipse living on our screen

class Thing {

  float x,y;
  boolean highlight;
  float r;

  Thing (float x_, float y_) {
    x = x_;
    y = y_;
    highlight = false;
    r = random(8) + 1;
  }

  void move() {
    x += random(-1,1);
    y += random(-1,1);
  }

  void render() {
    noStroke();
    if (highlight) fill(255);
    else fill(100);
    ellipse(x,y,r,r); 
  }

}
