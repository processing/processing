import processing.pdf.*; // Import PDF code

void setup() {
  size(600, 600);
  background(255);
}

void draw() {
  stroke(0, 20);
  strokeWeight(20);
  line(mouseX, 0, width - mouseY, height);
}

void keyPressed() {
  if (key == 'B' || key == 'b') { // When 'B' or 'b' is pressed,
    beginRecord(PDF, "lines.pdf"); // start recording to the PDF
    background(255); // Set a white background
  } else if (key == 'E' || key == 'e') { // When 'E' or 'e' is pressed,
    endRecord(); // stop recording the PDF and
    exit(); // quit the program
  }
}
