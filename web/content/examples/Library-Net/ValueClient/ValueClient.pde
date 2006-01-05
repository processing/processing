// Value Client
// by hbarragan

// Starts a network client that connects to a server in the local computer
// using the port 5204. It reads a gray value from the server and uses it 
// to set the background value. The example ValueServer must be running
// prior to starting this example.

// Created 23 June 2003
// Update 28 September 2004

import processing.net.*;

int data;  
Client client;

void setup() 
{
  size(200, 200);
  noStroke();
  client = new Client(this, "localhost", 5204);
  println(client.ip());
}

void draw() 
{
  background(0);

  if (client.available() > 0) {
    int f = client.read();
    fill(f);
  }
  rect(width/2 - 50, height/2 -50 , 100, 100);
}
