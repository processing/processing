// Sets the stroke color of the lines to the
// red component of the pixel below their intersection

void setup() {
  size(100, 100);
  smooth();
  fill(204, 0, 0);
}

void draw() {
  background(0);
  noStroke();
  ellipse(66, 46, 80, 80);
  color c = get(mouseX, mouseY);
  float r = red(c); // Extract red component
  stroke(255 - r); // Set the stroke based on red value
  line(mouseX, 0, mouseX, height);
  line(0, mouseY, width, mouseY);
}
