// Write data to the serial port according to the mouseX value
import processing.serial.*;

Serial port; // Create object from Serial class
float mx = 0.0;

void setup() {
  size(200, 200);
  noStroke();
  frameRate(10);
// Open the port that the board is connected to and use the same speed (9600 bps)
  port = new Serial(this, 9600);
}

void draw() {
  background(0); // Clear background
  fill(204); // Set fill color
  rect(40, height / 2 - 15, 120, 25); // Draw square
  float dif = mouseX - mx;
  if (abs(dif) > 1.0) {
    mx += dif / 4.0;
  }
  mx = constrain(mx, 50, 149); // Keeps marker on the screen
  noStroke();
  fill(255);
  rect(50, (height / 2) - 5, 100, 5);
  fill(204, 102, 0);
  rect(mx - 2, height / 2 - 5, 4, 5); // Draw the position marker
  int angle = int(map(mx, 50, 149, 0, 180)); // Scale the value the range 0-180
//print(angle + " "); // Print the current angle (debug)
  port.write(angle); // Write the angle to the serial port
}
