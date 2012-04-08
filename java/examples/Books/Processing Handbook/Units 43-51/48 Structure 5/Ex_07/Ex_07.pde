SpinSpots spots;
SpinArm arm;

void setup() {
  size(100, 100);
  smooth();
  arm = new SpinArm(width / 2, height / 2, 0.01);
  spots = new SpinSpots(width / 2, height / 2, -0.02, 33.0);
}

void draw() {
  background(204);
  arm.update();
  arm.display();
  spots.update();
  spots.display();
}
