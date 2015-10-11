import processing.io.*;
color bgcolor = 0;

// RPI.PIN3 refers to the physical pin 3 on the Raspberry Pi's
// pin header, which is located on the third row, next to a
// Ground pin

void setup() {
  GPIO.pinMode(RPI.PIN3, GPIO.INPUT);
  GPIO.attachInterrupt(RPI.PIN3, this, "pinEvent", GPIO.RISING);
}

void draw() {
  background(bgcolor);
}

// this function will be called whenever pin 3 is brought from LOW to HIGH
void pinEvent(int pin) {
  println("Received interrupt");
  if (bgcolor == 0) {
    bgcolor = color(255);
  } else {
    bgcolor = color(0);
  }
}
