class Spin {
  float x, y, speed;
  float angle = 0.0;

  Spin(float xpos, float ypos, float s) {
    x = xpos;
    y = ypos;
    speed = s;
  }

  void update() {
    angle += speed;
  }
}

