import processing.io.*;
MCP3001 adc;

void setup() {
  //printArray(SPI.list());
  adc = new MCP3001(SPI.list()[0]);
}

void draw() {
  background(adc.getAnalog() * 255);
}
