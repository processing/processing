// Fetch
// by Francis Li <http://www.francisli.com>
//
// This example demonstrates the basic usage of the PClient class for
// network communication.  It uses the GET() method to fetch an image
// from the Mobile Processing website and then displays it on the
// screen.
//
final int STATE_START       = 0;
final int STATE_DONE        = 1;
final int STATE_ERROR       = 2;

PClient client;
PFont font;
int state;
PImage img;

void setup() {
  client = new PClient("mobile.processing.org");
  font = loadFont();
  textFont(font);
  state = STATE_START;  
  softkey("GET");
  noLoop();
}

void softkeyPressed(String label) {
  if (label.equals("GET")) {
    if (client.GET("/images/mobile.png")) {
      byte[] data = client.readBytes();
      img = loadImage(data);      
      state = STATE_DONE;
    } else {
      state = STATE_ERROR;
    }
    redraw();
  }
}

void draw() {
  background(255);
  switch (state) {
    case STATE_START:
      fill(0);
      text("Press GET to start.", 4, 4, width - 8, height - 8);
      break;
    case STATE_DONE:
      image(img, 4, 4);
      break;
    case STATE_ERROR:
      text("An error has occurred.", 4, 4, width - 8, height - 8);
      break;
  }
}
