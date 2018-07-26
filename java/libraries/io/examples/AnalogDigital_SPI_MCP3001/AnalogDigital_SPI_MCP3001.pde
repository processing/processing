import processing.io.*;
MCP3001 adc;

// see setup.png in the sketch folder for wiring details

void setup() {
  //printArray(SPI.list());
  adc = new MCP3001(SPI.list()[0]);
}

void draw() {
  // this will return a number between 0 and 1
  float measured = adc.analogRead();

  // multiply with the supply voltage to get an absolute value
  float volts = 3.3 * measured;
  println("Analog Input is " + volts + "V");

  background(measured * 255);
}
