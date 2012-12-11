// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A random walker object!

class Walker {
  float x, y;

  Walker() {
    x = width/2;
    y = height/2;
  }

  void render() {
    stroke(255);
    point(x, y);
  }

  // Randomly move according to floating point values
  void step() {
    float stepx = random(-1, 1);
    float stepy = random(-1, 1);
    x += stepx;
    y += stepy;
    x = constrain(x, 0, width-1);
    y = constrain(y, 0, height-1);
  }
}

