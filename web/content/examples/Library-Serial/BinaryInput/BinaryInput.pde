// Binary Input
// by Mathias Dahlstrom <http://people.interaction-ivrea.it/m.dahlstrom>
  
//  Example of a binary input from a serial device using serial communcation.
//  This example takes input from an external serial device running at 19200 bps.
//  Examples for the BX-24 microcontroller and the PIC 18F452 
//  microcontroller (in PicBasic Pro) are given below.
//  More information on microcontrollers can be found on the tutorial 
//  pages of Tom Igoe: http://itp.nyu.edu/tigoe/pcomp/  
//  Because this program uses the serial port, it will not work within a web browser.

//  Created 12 February 2003
//  Updated 21 March 2005, T. Igoe

import processing.serial.*;

// State of the circle drawing
boolean circleExpanding = true;

// Size of the circle
float circleSize = 0;

Serial port;

void setup() 
{
  size(200, 200);
  
  // Print a list in case COM1 doesn't work out
  //println("Available serial ports:");
  //printarr(PSerial.list());
  
  //port = new Serial(this, "COM1", 19200);
  // Uses the first available port
  port = new Serial(this, Serial.list()[0], 19200);
  noStroke();
  fill(204);
  ellipseMode(CENTER_RADIUS);
}

// The loop checks for what state the circle should be 
// drawn into and performs the drawing.
void draw() 
{
  background(0);

  while (port.available() > 0) {
    int value = port.read();
    serialEvent(value);
  }

  if(circleExpanding) {
    ellipse(width/2, height/2, circleSize, circleSize);
    circleSize += 0.5;
  }else{
    ellipse(width/2, height/2, circleSize, circleSize);
    circleSize -= 0.5;
  }
  if(circleSize > width/2) {
    circleSize = width/2;
  }
  if(circleSize < 10) {
    circleSize = 10;
  }
}

// Function is called when ever a new byte from the 
// BX-24 is avaliable for reading.
// It controls what input the user is generating and 
// sets the corresponding drawing mode.
void serialEvent(int serial) 
{
  // Checks the ASCII code sent from the basicX chip. 
  // '48' is the code for '0' and '49' is the code for '1'
  if(serial == 48) {
    circleExpanding = false;
  }
  if(serial == 49) {
    circleExpanding = true;
  }
  
  println(serial);
}



// / / / / / / / / / / / / / / / / / / /

// Code for the BX-24 Microcontroller

/*
Option Explicit

dim inputVal as BYTE
dim lastVal as BYTE

Public Sub Main()
  Call PutPin(13,1)
  lastVal = 1
  Do
    inputVal = GetPin(13)
    If (inputVal <> lastVal) Then 
      If(inputVal = 0) Then
        debug.print "0"
      End if
      If(inputVal = 1) Then
        debug.print "1"
      End if			
    End if
    lastVal = inputVal
  Loop
End Sub
*/

//  Code for the PIC 18F452 in PicBasic Pro:

/*
ButtonStateVar var byte
LastButtonStateVar var byte
ButtonCountVar var byte
Input portc.5
ButtonCountvar = 0

main:
	ButtonStateVar = portc.5

	' if the button isn't the same as it was last time through
	' the main loop, then you want to do something:

	if buttonStateVar <> lastButtonStateVar then	
		if buttonStateVar = 1 then
			' the button went from off to on
			ButtonCountVar = ButtonCountVar + 1
			serout2 portc.6, 16416, ["1"]
			high portb.0
		else
		low portb.0
			' the button went from on to off
			serout2 portc.6, 16416, ["0"]
		endif
			
' store the state of the button for next check:
		lastButtonStateVar = buttonStateVar
	endif
goto main


*/
