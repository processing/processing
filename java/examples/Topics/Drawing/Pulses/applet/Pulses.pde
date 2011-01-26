/**
 * Pulses. 
 * 
 * Software drawing instruments can follow a rhythm or abide by rules independent
 * of drawn gestures. This is a form of collaborative drawing in which the draftsperson
 * controls some aspects of the image and the software controls others.
 */

int angle = 0;

void setup() {
  size(640, 360);
  background(102);
  smooth();
  noStroke();
  fill(0, 102);
}

void draw() {
  // Draw only when mouse is pressed
  if (mousePressed == true) {
    angle += 10;
    float val = cos(radians(angle)) * 6.0;
    for (int a = 0; a < 360; a += 75) {
      float xoff = cos(radians(a)) * val;
      float yoff = sin(radians(a)) * val;
      fill(0);
      ellipse(mouseX + xoff, mouseY + yoff, val, val);
    }
    fill(255);
    ellipse(mouseX, mouseY, 2, 2);
  }
}
