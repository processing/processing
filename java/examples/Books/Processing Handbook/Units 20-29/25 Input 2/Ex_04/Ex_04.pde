void setup() {
  size(100, 100);
  smooth();
  strokeWeight(4);
}

void draw() {
  background(204);
  // If the 'A' key is pressed draw a line
  if ((keyPressed == true) && (key == 'A')) {
    line(50, 25, 50, 75);
  } else { // Otherwise, draw an ellipse
    ellipse(50, 50, 50, 50);
  }
}