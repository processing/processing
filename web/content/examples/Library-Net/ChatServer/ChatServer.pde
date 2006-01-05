// Chat Server
// by Tom Igoe

//

// Created 18 March 2005

import processing.net.*;

PFont font;
int port = 10002;
boolean myServerRunning = true;
int bgColor = 0;
int direction = 1;
int textLine = 60;

Server myServer;

void setup()
{
  size(400, 400);
  background(0);
  font = loadFont("ScalaSans-Caps-32.vlw");
  textFont(font, 18);
  myServer = new Server(this, port); // Starts a myServer on port 10002
  background(0);
}

public void mouseClicked()
{
  // If the mouse clicked the myServer stops
  myServer.stop();
  myServerRunning = false;
}

void draw()
{

  if (myServerRunning == true)
  {
    text("server", 15, 25);
    Client thisClient = myServer.available();
    if (thisClient != null) {
      if (thisClient.available() > 0) {
        text("mesage from: " + thisClient.ip() + " : " + thisClient.readString(), 15, textLine);
        textLine = textLine + 35;
      }
    }
  } 
  else 
  {
    text("server", 15, 60);
    text("stopped", 15, 95);
  }

}
