import processing.core.*; public class fetch extends PMIDlet{// Fetch
// by Francis Li <http://www.francisli.com>
//
// This example demonstrates the basic usage of the PClient class for
// network communication.  It uses the GET() method to fetch an image
// from the Mobile Processing website and then displays it on the
// screen.
//
PClient client;
PRequest request;
PFont font;
PImage img;
String msg;

public void setup() {
  // instantiate a new PClient object to connect to the Mobile Procesing website
  client = new PClient(this, "mobile.processing.org");
  // set up the font for displaying text
  font = loadFont();
  textFont(font);
  // create a softkey command for initiating the network request
  softkey("GET");
  // we're not animating, so turn off draw loop
  noLoop();
}

public void softkeyPressed(String label) {
  if (label.equals("GET")) {
    // initiate the request
    request = client.GET("/images/mobile.png");
    // remove the softkey command
    softkey(null);
    // update the screen
    redraw();
  }
}

public void libraryEvent(Object library, int event, Object data) {
  if (library == request) {
    if (event == PRequest.EVENT_CONNECTED) {
      // connected, start reading the data
      request.readBytes();
      redraw();
    } else if (event == PRequest.EVENT_DONE) {
      // done reading, create the image
      img = loadImage((byte[]) data);
      request.close();
      redraw();
    } else if (event == PRequest.EVENT_ERROR) {
      // an error occurred, get the error message
      msg = (String) data;
      redraw();
    }
  }
}

public void draw() {
  background(255);
  fill(0);
  if (img != null) {
    // show the image
    image(img, 4, 4);
  } else if (request == null) {
    // prompt for the network request
    text("Press GET to start.", 4, 4, width - 8, height - 8);    
  } else {
    // handle the network states
    switch (request.state) {
        case PRequest.STATE_OPENED:
          text("Connecting...", 4, 4, width - 8, height - 8);
          break;
        case PRequest.STATE_FETCHING:
          text("Fetching...", 4, 4, width - 8, height - 8);
          break;
        case PRequest.STATE_ERROR:
          text("An error has occurred: " + msg, 4, 4, width - 8, height - 8);
          break;
    }
  }
}
}