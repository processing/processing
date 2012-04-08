class SpinArm extends Spin {
  SpinArm(float x, float y, float s) {
    super(x, y, s);
  }

  void display() {
    strokeWeight(1);
    stroke(0);
    pushMatrix();
    translate(x, y);
    angle += speed;
    rotate(angle);
    line(0, 0, 100, 0);
    popMatrix();
  }
}
