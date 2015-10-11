import processing.io.*;
boolean ledOn = false;

// RPI.PIN5 refers to the physical pin 5 on the Raspberry Pi's
// pin header, which is located on the third row, next to a
// Ground pin

void setup() {
  GPIO.pinMode(RPI.PIN5, GPIO.OUTPUT);
  // this is equivalent to addressing the pin with its GPIO number:
  // GPIO.pinMode(3, GPIO.OUTPUT);
  frameRate(0.5);
}

void draw() {
  // make the LED blink
  ledOn = !ledOn;
  if (ledOn) {
    GPIO.digitalWrite(RPI.PIN5, GPIO.LOW);
    fill(204);
  } else {
    GPIO.digitalWrite(RPI.PIN5, GPIO.HIGH);
    fill(255);
  }
  stroke(255);
  ellipse(width/2, height/2, width*0.75, height*0.75);
}
