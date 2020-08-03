import processing.io.*;
color bgcolor = 0;

// GPIO numbers refer to different phyiscal pins on various boards
// On the Raspberry Pi GPIO 4 is physical pin 7 on the header
// see setup.png in the sketch folder for wiring details

void setup() {
  GPIO.pinMode(4, GPIO.INPUT);
  GPIO.attachInterrupt(4, this, "pinEvent", GPIO.RISING);
}

void draw() {
  background(bgcolor);
}

// this function will be called whenever GPIO 4 is brought from LOW to HIGH
void pinEvent(int pin) {
  println("Received interrupt");
  if (bgcolor == 0) {
    bgcolor = color(255);
  } else {
    bgcolor = color(0);
  }
}
