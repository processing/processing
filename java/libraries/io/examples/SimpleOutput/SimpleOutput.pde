import processing.io.*;
boolean ledOn = false;

// GPIO numbers refer to different phyiscal pins on various boards
// On the Raspberry Pi GPIO 4 is physical pin 7 on the header
// see setup.png in the sketch folder for wiring details

void setup() {
  GPIO.pinMode(4, GPIO.OUTPUT);
  frameRate(0.5);
}

void draw() {
  // make the LED blink
  ledOn = !ledOn;
  if (ledOn) {
    GPIO.digitalWrite(4, GPIO.LOW);
    fill(204);
  } else {
    GPIO.digitalWrite(4, GPIO.HIGH);
    fill(255);
  }
  stroke(255);
  ellipse(width/2, height/2, width*0.75, height*0.75);
}
