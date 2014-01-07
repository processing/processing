/**
 * Many Serial Ports
 * 
 * Read data from the multiple Serial Ports
 */


import processing.serial.*;

Serial[] myPorts = new Serial[2];  // Create a list of objects from Serial class
int[] dataIn = new int[2];         // a list to hold data from the serial ports

void setup()  {
  size(400, 300);
  // print a list of the serial ports:
  printArray(Serial.list());
  // On my machine, the first and third ports in the list
  // were the serial ports that my microcontrollers were 
  // attached to.
  // Open whatever ports ares the ones you're using.

// get the ports' names:
  String portOne = Serial.list()[0];
  String portTwo = Serial.list()[2];
  // open the ports:
  myPorts[0] = new Serial(this, portOne, 9600);
  myPorts[1] = new Serial(this, portTwo, 9600);
}


void draw() {
  // clear the screen:
  background(0);
  // use the latest byte from port 0 for the first circle
  fill(dataIn[0]);
  ellipse(width/3, height/2, 40, 40);
    // use the latest byte from port 1 for the second circle
   fill(dataIn[1]);
  ellipse(2*width/3, height/2, 40, 40);
}

/** 
  * When SerialEvent is generated, it'll also give you
  * the port that generated it.  Check that against a list
  * of the ports you know you opened to find out where
  * the data came from
*/
void serialEvent(Serial thisPort) {
  // variable to hold the number of the port:
  int portNumber = -1;
  
  // iterate over the list of ports opened, and match the 
  // one that generated this event:
  for (int p = 0; p < myPorts.length; p++) {
    if (thisPort == myPorts[p]) {
      portNumber = p;
    }
  }
  // read a byte from the port:
  int inByte = thisPort.read();
  // put it in the list that holds the latest data from each port:
  dataIn[portNumber] = inByte;
  // tell us who sent what:
  println("Got " + inByte + " from serial port " + portNumber);
}

/*
The following Wiring/Arduino code runs on both microcontrollers that
were used to send data to this sketch:

void setup()
{
  // start serial port at 9600 bps:
  Serial.begin(9600);
}

void loop() {
  // read analog input, divide by 4 to make the range 0-255:
  int analogValue = analogRead(0)/4; 
  Serial.write(analogValue);
  // pause for 10 milliseconds:
  delay(10);                 
}


*/
