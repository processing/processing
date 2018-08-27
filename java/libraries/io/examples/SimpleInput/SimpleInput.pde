import processing.io.*;

// GPIO numbers refer to different phyiscal pins on various boards
// On the Raspberry Pi GPIO 4 is physical pin 7 on the header
// see setup.png in the sketch folder for wiring details

void setup() {
  // INPUT_PULLUP enables the built-in pull-up resistor for this pin
  // left alone, the pin will read as HIGH
  // connected to ground (via e.g. a button or switch) it will read LOW
  GPIO.pinMode(4, GPIO.INPUT_PULLUP);
}

void draw() {
  if (GPIO.digitalRead(4) == GPIO.LOW) {
    // button is pressed
    fill(255);
  } else {
    // button is not pressed
    fill(204);
  }
  stroke(255);
  ellipse(width/2, height/2, width*0.75, height*0.75);
}
