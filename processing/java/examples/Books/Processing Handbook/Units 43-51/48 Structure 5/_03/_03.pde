EggRing er1, er2;

void setup() {
  size(100, 100);
  smooth();
  er1 = new EggRing(33, 66, 0.1, 33);
  er2 = new EggRing(66, 90, 0.05, 66);
}

void draw() {
  background(0);
  er1.transmit();
  er2.transmit();
}
