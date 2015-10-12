import processing.io.*;

// RPI.PIN7 refers to the physical pin 7 on the Raspberry Pi's
// pin header, which is located on the fourth row, above one of
// the Ground pins

void setup() {
  GPIO.pinMode(RPI.PIN7, GPIO.INPUT);
  // this is equivalent to addressing the pin with its GPIO number:
  // GPIO.pinMode(4, GPIO.INPUT);
}

void draw() {
  // sense the input pin
  if (GPIO.digitalRead(RPI.PIN7) == GPIO.HIGH) {
    fill(255);
  } else {
    fill(204);
  }
  stroke(255);
  ellipse(width/2, height/2, width*0.75, height*0.75);
}
