float angle = 0;

void setup() {
  size(100, 100);
  smooth();
  strokeWeight(8);
}

void draw() {
  background(204);
  if (keyPressed == true) {
    if ((key >= 32) && (key <= 126)) {
      // If the key is alphanumeric,
      // convert its value into an angle
      angle = map(key, 32, 126, 0, TWO_PI);
    }
  }
  arc(50, 50, 66, 66, angle-PI/6, angle+PI/6);
}