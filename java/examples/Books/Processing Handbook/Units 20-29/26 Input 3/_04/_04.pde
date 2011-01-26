int dragX, dragY, moveX, moveY;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(204);
  fill(0);
  ellipse(dragX, dragY, 33, 33); // Black circle
  fill(153);
  ellipse(moveX, moveY, 33, 33); // Gray circle
}

void mouseMoved() { // Move gray circle
  moveX = mouseX;
  moveY = mouseY;
}

void mouseDragged() { // Move black circle
  dragX = mouseX;
  dragY = mouseY;
}