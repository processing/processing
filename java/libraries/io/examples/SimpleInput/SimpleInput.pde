import processing.io.*;

// RPI.PIN3 refers to the physical pin 3 on the Raspberry Pi's
// pin header, which is located on the second row, next to the
// 5v power pin

void setup() {
  GPIO.pinMode(RPI.PIN3, GPIO.INPUT);
  // this is equivalent to addressing the pin with its GPIO number:
  // GPIO.pinMode(2, GPIO.INPUT);
}

void draw() {
  // sense the input pin
  if (GPIO.digitalRead(RPI.PIN3) == GPIO.HIGH) {
    fill(255);
  } else {
    fill(204);
  }
  stroke(255);
  ellipse(width/2, height/2, width*0.75, height*0.75);
}
