// Save the first 50 frames
float x = 33;
float numFrames = 50;

void setup() {
  size(100, 100);
  smooth();
  noStroke();
}

void draw() {
  background(0);
  x += random(-2, 2);
  ellipse(x, 50, 40, 40);
  if (frameCount <= numFrames) {
    saveFrame("circles-####.tif");
  }
}
