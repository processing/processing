import processing.io.*;
MCP3008 adc;

// see setup.png in the sketch folder for wiring details

void setup() {
  //printArray(SPI.list());
  adc = new MCP3008(SPI.list()[0]);
}

void draw() {
  // this will return a number between 0 and 1
  float measured = adc.analogRead(0);

  // multiply with the supply voltage to get an absolute value
  float volts = 3.3 * measured;
  println("Analog Input 0 is " + volts + "V");

  background(255);
  fill(measured * 255);
  ellipse(width/2, height/2, width * 0.75, width * 0.75);
}
