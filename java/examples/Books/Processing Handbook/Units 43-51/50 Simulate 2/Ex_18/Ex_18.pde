class FixedSpring extends Spring2D {
  float springLength;

  FixedSpring(float xpos, float ypos, float m, float g, float s) {
    super(xpos, ypos, m, g);
    springLength = s;
  }

  void update(float newX, float newY) {
    // Calculate the target position
    float dx = x - newX;
    float dy = y - newY;
    float angle = atan2(dy, dx);
    float targetX = newX + cos(angle) * springLength;
    float targetY = newY + sin(angle) * springLength;

    // Activate update method from Spring2D
    super.update(targetX, targetY);

    // Constrain to display window
    x = constrain(x, radius, width - radius);
    y = constrain(y, radius, height - radius);
  }
}
