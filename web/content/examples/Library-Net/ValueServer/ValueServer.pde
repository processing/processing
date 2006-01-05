// Value Server
// by hbarragan

// Starts a network server on the local computer using the port 5204. 
// It sends the connected clients values between 0 and 255. Click the 
// mouse to stop the server.

// Created 23 June 2003
// Updated 15 April 2005

import processing.net.*;

PFont font;
int port = 5204;
boolean myServerRunning = true;
int bgColor = 0;
int direction = 1;

Server myServer;

void setup()
{
  size(200, 200);
  background(0);
  font = loadFont("ScalaSans-Caps-32.vlw");
  textFont(font, 32);
  myServer = new Server(this, port); // Starts a myServer on port 5204
}

public void mouseClicked()
{
  // If the mouse clicked the myServer stops
  myServer.stop();
  myServerRunning = false;
}

void draw()
{
  background(0);
  if (myServerRunning == true)
  {
    text("server", 15, 60);
    text("sending", 15, 95);
    text("color " + bgColor, 15, 130);
  }
  else
  {
    text("server", 15, 60);
    text("stopped", 15, 95);
  }
  myServer.write(bgColor);

  bgColor = bgColor + 1 * direction ;
  if ((bgColor == 0) || (bgColor == 255)) {
    direction *=-1;
  }
}
