// Example 11-08 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

import processing.serial.*;

Serial port;  // Create object from Serial class
float val;    // Data received from the serial port
int x;
float easing = 0.05;
float easedVal;

void setup() {
  size(440, 440);
  frameRate(30);
  smooth();
  String arduinoPort = Serial.list()[0];
  port = new Serial(this, arduinoPort, 9600);
  background(0);
}

void draw() {
  if ( port.available() > 0) {  // If data is available,
    val = port.read();          // read it and store it in val
    val = map(val, 0, 255, 0, height);  // Convert the values    
  }

  float targetVal = val;
  easedVal += (targetVal - easedVal) * easing;

  stroke(0);
  line(x, 0, x, height);  // Black line
  stroke(255);
  line(x+1, 0, x+1, height);  // White line
  line(x, 220, x, val);  // Raw value
  line(x, 440, x, easedVal + 220); // Averaged value

  x++;
  if (x > width) {
    x = 0; 
  }
}



