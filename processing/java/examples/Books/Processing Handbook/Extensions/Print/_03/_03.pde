import processing.pdf.*; // Import PDF code

boolean saveOneFrame = false;

void setup() {
  size(600, 600);
}

void draw() {
  if (saveOneFrame == true) { // When the saveOneFrame boolean is true,
    beginRecord(PDF, "line-####.pdf"); // start recording to the PDF
  }
  background(255);
  stroke(0, 20);
  strokeWeight(20);
  line(mouseX, 0, width - mouseY, height);
  if (saveOneFrame == true) { // If the PDF has been recording,
    endRecord(); // stop recording,
    saveOneFrame = false; // and set the boolean value to false
  }
}

void mousePressed() { // When a mouse button is pressed,
  saveOneFrame = true; // trigger PDF recording within the draw()
}
