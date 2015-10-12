import processing.io.*;
color bgcolor = 0;

// RPI.PIN7 refers to the physical pin 7 on the Raspberry Pi's
// pin header, which is located on the fourth row, above one of
// the Ground pins

void setup() {
  GPIO.pinMode(RPI.PIN7, GPIO.INPUT);
  GPIO.attachInterrupt(RPI.PIN7, this, "pinEvent", GPIO.RISING);
}

void draw() {
  background(bgcolor);
}

// this function will be called whenever pin 7 is brought from LOW to HIGH
void pinEvent(int pin) {
  println("Received interrupt");
  if (bgcolor == 0) {
    bgcolor = color(255);
  } else {
    bgcolor = color(0);
  }
}
