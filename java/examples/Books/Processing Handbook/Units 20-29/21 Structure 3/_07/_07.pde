void setup() {
  size(100, 100);
  noLoop();
  smooth();
}

void draw() {
  drawX(0); // Passes 0 to drawX(), runs drawX()
}

void drawX(int gray) { // Declares and assigns gray
  stroke(gray); // Uses gray to set the stroke
  strokeWeight(20);
  line(0, 5, 60, 65);
  line(60, 5, 0, 65);
}
