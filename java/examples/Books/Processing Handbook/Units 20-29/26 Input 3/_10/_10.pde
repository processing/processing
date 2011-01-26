void setup() {
  size(100, 100);
  noLoop();
}

void draw() {
  background(204);
  line(mouseX, 0, mouseX, 100);
}

void mousePressed() {
  redraw(); // Run the code in draw one time
}