Blade diagonal;

void setup() {
  size(100, 100);
  diagonal = new Blade(30, 80);
}

void draw() {
  diagonal.grow();
}

void mouseMoved() {
  diagonal.seed(mouseX, mouseY);
}

class Blade {
  float x, y;
  Blade(int xpos, int ypos) {
    x = xpos;
    y = ypos;
  }
  void seed(int xpos, int ypos) {
    x = xpos;
    y = ypos;
  }
  void grow() {
    x += 0.5;
    y -= 1.0;
    point(x, y);
  }
}
