class SpinSpots extends Spin {
  float dim;

  SpinSpots(float x, float y, float s, float d) {
    super(x, y, s);
    dim = d;
  }

  void display() {
    noStroke();
    pushMatrix();
    translate(x, y);
    angle += speed;
    rotate(angle);
    ellipse(-dim / 2, 0, dim, dim);
    ellipse(dim / 2, 0, dim, dim);
    popMatrix();
  }
}
