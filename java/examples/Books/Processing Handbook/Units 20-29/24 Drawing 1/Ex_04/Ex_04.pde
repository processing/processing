// Draw lines with different gray values when a mouse
// button is pressed or not pressed
void setup() {
  size(100, 100);
}

void draw() {
  if (mousePressed == true) { // If mouse is pressed,
    stroke(255); // set the stroke to white
  } else { // Otherwise,
    stroke(0); // set to black
  }
  line(mouseX, mouseY, pmouseX, pmouseY);
}
