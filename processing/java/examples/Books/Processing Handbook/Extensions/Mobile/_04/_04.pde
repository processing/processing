// The PClient object is used to initiate requests to the server
PClient c;
// The PRequest object represents an active request from which we receive
// status information and data from the server
PRequest request;
int counter;
PFont font;
PImage img;
String version;
String error;

void setup() {
  font = loadFont(); // Load and set the default font for drawing text
  textFont(font);
  fill(0);
// Create a new network connection to connect to the Mobile Processing website
  c = new PClient(this, "mobile.processing.org");
// Start by fetching the logo for Mobile Processing the filename is a relative path
// specified in the same way as a URL in a webpage
  request = c.GET("/images/mobile.png");
// Use the counter to keep track of what we're fetching
  counter = 0;
}

void draw() {
  background(255);
  int y = 0;
  if (error != null) {
// A network error has occured, so display the message
    y += font.baseline;
    text(error, 0, y);
  } else if (img == null) {
// The img is not yet fetched, so draw a status message
    y += font.baseline;
    text("Fetching image...", 0, y);
  } else {
// Draw the image
    image(img, (width - img.width) / 2, y);
    y += img.height + font.baseline;
    if (version == null) {
// The version text is not yet fetched, so draw a status message
      text("Checking version...", 0, y);
    } else {
// Draw the version as reported by the website
      text("Latest version: " + version, 0, y);
    }
  }
}
// The libraryEvent() will be called when a library, in this case the Net
// library, has an event to report back to the program
void libraryEvent(Object library, int event, Object data) {
// Make sure we handle the event from the right library
  if (library == request) {
    if (event == PRequest.EVENT_CONNECTED) {
// This event occurs when the connection is complete, so we can start
// reading the data. The readBytes() method will read all the data returned
// by the server and send another event when completed.
      request.readBytes();
    } else if (event == PRequest.EVENT_DONE) {
// Reading is complete! Check the counter to see what we're transferring,
// then process the data. The data object in this case is an array of bytes.
      byte[] bytes = (byte[]) data;
      if (counter == 0) {
// This is the logo, so create an image from the bytes
        img = new PImage(bytes);
// Now that we have the logo image, fetch the latest version text for
// Mobile Processing. We use the client object to initiate a new request
        request = c.GET("/download/latest.txt");
// Set the counter to 1 to represent the tex
        counter = 1;
      } else if (counter == 1) {
// This is the version text, so create a string from the bytes
        version = new String(bytes);
      }
    } else if (event == PRequest.EVENT_ERROR) {
// The data object in this case is an error message
      error = (String) data;
    }
  }
}
