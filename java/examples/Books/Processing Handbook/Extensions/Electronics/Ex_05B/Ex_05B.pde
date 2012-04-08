// Write data to the serial port according to the status of a button controlled
// by the mouse
import processing.serial.*;
Serial port; // Create serial port object
boolean rectOver = false;
int rectX, rectY; // Position of square button
int rectSize = 100; // Diameter of rect
color rectColor;
boolean buttonOn = false; // Status of the button

void setup() {
  size(200, 200);
  noStroke();
  frameRate(10);
  rectColor = color(100);
  rectX = width / 2 - rectSize / 2;
  rectY = height / 2 - rectSize / 2;
// Open the port that the board is connected to and use the same speed (9600 bps)
  port = new Serial(this, 9600);
}

void draw() {
  update(mouseX, mouseY);
  background(0); // Clear background to black
  fill(rectColor);
  rect(rectX, rectY, rectSize, rectSize);
}
void update(int x, int y) {
  if (overRect(rectX, rectY, rectSize, rectSize) == true) {
    rectOver = true;
  } else {
    rectOver = false;
  }
}

void mouseReleased() {
  if (rectOver == true) {
    if (buttonOn == true) {
      rectColor = color(100);
      buttonOn = false;
      port.write('L'); // Send an L to indicate button is OFF
    } else {
      rectColor = color(180);
      buttonOn = true;
      port.write('H'); // Send an H to indicate button is ON
    }
  }
}
boolean overRect(int x, int y, int width, int height) {
  if ((mouseX >= x) && (mouseX <= x + width) &&
      (mouseY >= y) && (mouseY <= y + height)) {
    return true;
  } else {
    return false;
  }
}
