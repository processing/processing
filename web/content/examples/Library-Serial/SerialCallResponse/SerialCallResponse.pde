// Serial call-and-response for v.85
// by Tom Igoe

// Sends a byte out the serial port, and reads 3 bytes in.
// Sets foregound color, xpos, and ypos of a circle onstage
// using the values returned from the serial port.
// Thanks to Daniel Shiffman for the improvements.

// Updated 21 March 2005

import processing.serial.*;

int bgcolor;			     // Background color
int fgcolor;			     // Fill color
Serial port;                         // The serial port
int[] serialInArray = new int[3];    // Where we'll put what we receive
int serialCount = 0;                 // A count of how many bytes we receive
float xpos, ypos;		     // Starting position of the ball
boolean firstContact = false;        // Whether we've heard from the microcontroller

void setup() {
  size(256, 256);  // Stage size
  noStroke();      // No border on the next thing drawn

  // Set the starting position of the ball (middle of the stage)
  xpos = width/2;
  ypos = height/2;

  // Print a list of the serial ports, for debugging purposes:
  println(Serial.list());

  // I know that the first port in the serial list on my mac
  // is always my  Keyspan adaptor, so I open Serial.list()[0].
  // On Windows machines, this generally opens COM1.
  // Open whatever port is the one you're using.
  port = new Serial(this, Serial.list()[0], 9600);
  port.write(65);    // Send a capital A to start the microcontroller sending
}

void draw() {
  background(bgcolor);
  fill(fgcolor);
  // Draw the shape
  ellipse(xpos, ypos, 20, 20);
  // Get any new serial data
  while (port.available() > 0) {
    serialEvent();
    // Note that we heard from the microntroller:
    firstContact = true;
  }
  // If there's no serial data, send again until we get some.
  // (in case you tend to start Processing before you start your 
  // external device):
  if (firstContact == false) {
    delay(300);
    port.write(65);
  }
}

void serialEvent() {
  processByte((char)port.read());
}

void processByte(char inByte) {
  // Add the latest byte from the serial port to array:
  serialInArray[serialCount] = inByte;
  serialCount++;
  // If we have 3 bytes:
  if (serialCount > 2 ) {
    xpos = (float)serialInArray[0];
    ypos = (float)serialInArray[1];
    fgcolor = (int)serialInArray[2];
    // Send a capital A to request new sensor readings:
    port.write(65);
    // Reset serialCount:
    serialCount = 0;
  }
}
