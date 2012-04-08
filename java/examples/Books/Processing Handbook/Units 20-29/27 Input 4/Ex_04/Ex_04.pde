// The distance between the center of the display
// window and the cursor sets the diameter of the circle
void setup() {
  size(100, 100);
  smooth();
}

void draw() {
  background(0);
  float d = dist(width/2, height/2, mouseX, mouseY);
  ellipse(width/2, height/2, d*2, d*2);
}