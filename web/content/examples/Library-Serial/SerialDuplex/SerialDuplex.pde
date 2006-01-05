// Serial Duplex
// by Tom Igoe

// Sends a byte out the serial port, every 300ms,
// listens for bytes received, and displays their ASCII value.
// This is just a quick application for testing serial data
// in both directions.

// Created 20 April 2005

import processing.serial.*;

Serial port;        // The serial port
int thisByte = -1;  // Variable to hold keystoke values
int whichKey = -1;  // Incoming serial data
PFont fontA;        // Font for printing

void setup() {

  size(200, 200);

  // Load and prep fonts to print to window
  fontA = loadFont("CourierNewPSMT-24.vlw");
  textFont(fontA, 24);

  // List all the available serial ports:
  println(Serial.list());

  // I know that the first port in the serial list on my mac
  // is always my  Keyspan adaptor, so I open Serial.list()[0].
  // In Windows, this usually opens COM1.
  // Open whatever port is the one you're using.
  port = new Serial(this, Serial.list()[0], 9600);

  // send a capital A out the serial port:
  port.write(65);
}

void draw() {
  background(0);
  text("Received: " + thisByte, 10, 130);
  text("Sent: " + whichKey, 10, 100);
  // If there are bytes available in the input buffer,
  // Read them and print them:
  while (port.available() > 0) {
    thisByte = port.read();
  }
}

void keyPressed() {
  // Send the keystroke out:
  port.write(key);
  //000port.write(0);
  whichKey = key;
}


/*

I often test with PIC microcontrollers
using Microengineering Labs' PicBasic Pro.

Here's a test program that takes whatever you typed
and sends back the next value higher.

inByte var byte

main:
  serin2 portc.7, 16468, [inByte]
  serout2 portc.6, 16468, [inbyte+1]
goto main

*/

