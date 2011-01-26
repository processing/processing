// Check if the mouse is over a rectangle and writes the status to the serial port
import processing.serial.*;

Serial port; // Create object from Serial class

void setup() {
  size(200, 200);
  noStroke();
  frameRate(10);
// Open the port that the board is connected to and use the same speed (9600 bps)
  port = new Serial(this, 9600);
}

void draw() {
  background(255);
  if (mouseOverRect() == true) { // If mouse is over square,
    fill(204); // change color and
    port.write('H'); // send an H to indicate mouse is over square
  } else { // If mouse is not over square,
    fill(0); // change color and
    port.write('L'); // send an L otherwise
  }
  rect(50, 50, 100, 100); // Draw a square
}

boolean mouseOverRect() { // Test if mouse is over square
  return ((mouseX >= 50) && (mouseX <= 150) && (mouseY >= 50) && (mouseY <= 150));
}