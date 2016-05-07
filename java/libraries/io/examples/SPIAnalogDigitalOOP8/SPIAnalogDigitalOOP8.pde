import processing.io.*;
MCP3008 adc;

// see setup.png in the sketch folder for wiring details

void setup() {
  //printArray(SPI.list());
  adc = new MCP3008(SPI.list()[0]);
}

void draw() {
  background(adc.getAnalog(0) * 255);
  fill(adc.getAnalog(1) * 255);
  ellipse(width/2, height/2, width * 0.75, width * 0.75);
}
