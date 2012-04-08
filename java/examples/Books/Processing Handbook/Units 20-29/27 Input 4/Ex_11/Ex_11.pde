// The angles increase as the mouse moves from the upper-right
// corner of the screen to the lower-left corner

void setup() {
  size(100, 100);
  frameRate(15);
  fill(0);
}

void draw() {
  float angle = atan2(mouseY, mouseX);
  float deg = degrees(angle);
  println(deg);
  background(204);
  ellipse(mouseX, mouseY, 8, 8);
  rotate(angle);
  line(0, 0, 150, 0);
}