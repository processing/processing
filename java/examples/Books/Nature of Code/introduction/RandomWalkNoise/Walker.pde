// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A random walker object!

class Walker {
  float x, y;
  float tx, ty;

  float prevX, prevY;

  Walker() {
    tx = 0;
    ty = 10000;
    x = map(noise(tx), 0, 1, 0, width);
    y = map(noise(ty), 0, 1, 0, height);
  }

  void render() {
    stroke(255);
    line(prevX, prevY, x, y);
  }

  // Randomly move according to floating point values
  void step() {

    prevX = x;
    prevY = y;

    x = map(noise(tx), 0, 1, 0, width);
    y = map(noise(ty), 0, 1, 0, height);

    tx += 0.01;
    ty += 0.01;

  }
}

