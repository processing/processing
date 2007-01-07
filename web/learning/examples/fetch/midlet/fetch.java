import processing.core.*; public class fetch extends PMIDlet{// Fetch
// by Francis Li <http://www.francisli.com>
//
// This example demonstrates the basic usage of the PClient class for
// network communication.  It uses the GET() method to fetch an image
// from the Mobile Processing website and then displays it on the
// screen.
//
final int STATE_START       = 0;
final int STATE_CONNECTING  = 1;
final int STATE_FETCHING    = 2;
final int STATE_DONE        = 3;
final int STATE_ERROR       = 4;

PClient2 client;
PRequest request;
PFont font;
int state;
PImage img;

public void setup() {
  client = new PClient2(this, "localhost");//"mobile.processing.org");
  font = loadFont();
  textFont(font);
  state = STATE_START;  
  softkey("GET");
  noLoop();
}

public void softkeyPressed(String label) {
  if (label.equals("GET")) {
    request = client.GET("/processing/images/mobile.png");
    state = STATE_CONNECTING;
    softkey(null);
    redraw();
  }
}

public void libraryEvent(Object library, int event, Object data) {
  if (library == request) {
    if (event == PRequest.EVENT_CONNECTED) {
      request.readBytes();
      state = STATE_FETCHING;
      redraw();
    } else if (event == PRequest.EVENT_DONE) {
      img = loadImage((byte[]) data);
      state = STATE_DONE;
      redraw();
    } else if (event == PRequest.EVENT_ERROR) {
      state = STATE_ERROR;
      redraw();
    }
  }
}

public void draw() {
  background(255);
  fill(0);
  switch (state) {
    case STATE_START:
      text("Press GET to start.", 4, 4, width - 8, height - 8);
      break;
    case STATE_CONNECTING:
      text("Connecting...", 4, 4, width - 8, height - 8);
      break;
    case STATE_FETCHING:
      text("Fetching...", 4, 4, width - 8, height - 8);
      break;
    case STATE_DONE:
      image(img, 4, 4);
      break;
    case STATE_ERROR:
      text("An error has occurred.", 4, 4, width - 8, height - 8);
      break;
  }
}
}