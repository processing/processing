// Analog In 
// by Josh Nimoy <http://itp.jtnimoy.com>

// Reads a value from the serial port and sets the background color. 
// Running this example requires you have a BX-24 microcontroller
// and peripheral hardware. More information can be found on the tutorial 
// pages of Tom Igoe: http://stage.itp.nyu.edu/~tigoe/pcomp/examples.shtml
// Because this program uses the serial port, it will not work within a web browser.

// Created 8 February 2003
// Updated 2 April 2005

import processing.serial.*;

String buff = "";
int val = 0;
int NEWLINE = 10;

Serial port;

void setup()
{
  size(200, 200);

  // Print a list in case COM1 doesn't work out
  println("Available serial ports:");
  println(Serial.list());

  //port = new Serial(this, "COM1", 19200);
  // Uses the first available port
  port = new Serial(this, Serial.list()[0], 19200);
}

void draw()
{
  while (port.available() > 0) {
    serialEvent(port.read());
  }
  background(val);
}

void serialEvent(int serial) 
{ 
  // If the variable "serial" is not equal to the value for 
  // a new line, add the value to the variable "buff". If the 
  // value "serial" is equal to the value for a new line,
  //  save the value of the buffer into the variable "val".
  if(serial != NEWLINE) { 
    buff += char(serial);
  } else {
    buff = buff.substring(0, buff.length()-1);
    // Parse the String into an integer
    val = Integer.parseInt(buff)/4;
    println(val);
    // Clear the value of "buff"
    buff = "";
  }
}


// / / / / / / / / / / / / / / / / / / /

// Code for the BX-24 Microcontroller

/*
Sub main()
  call delay(0.5)  ' start with half-second delay 
  do
    ' returns values between 0 and 1023
    ' the variable resistor is in pin 18
    debug.print cstr(getADC(18))
    call delay(0.1)  ' one-tenth second delay
  loop
end sub
*/

