void draw() {
  for (int y = 20; y < 80; y += 6) { // The variable y can be used
    line(20, y, 50, y); // only within the for block
  }
  line(y, 0, y, 100); // ERROR! y can't be accessed outside for
}