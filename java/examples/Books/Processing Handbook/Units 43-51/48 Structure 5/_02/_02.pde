// Requires Egg and Ring classes (codes 43-08 and 43-11)
class EggRing {
  Egg ovoid;
  Ring circle = new Ring();

  EggRing(int x, int y, float t, float sp) {
    ovoid = new Egg(x, y, t, sp);
    circle.start(x, y - sp / 2);
  }

  void transmit() {
    ovoid.wobble();
    ovoid.display();
    circle.grow();
    circle.display();
    if (circle.on == false) {
      circle.on = true;
    }
  }
}
