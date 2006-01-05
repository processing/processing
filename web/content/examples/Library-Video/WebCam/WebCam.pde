// WebCam
// by REAS <http://reas.com>

// Reading and displaying an image from an
// attached Capture device.

// Updated 8 April 2005

import processing.video.*;

Capture camera;

void setup()
{
  size(200, 200);

  // List all available capture devices to the console
  // Use the information printed to the text area to
  // correctly set the variable "s" below
  println(Capture.list());

  // Specify your own device by the name of the capture
  // device returned from the Capture.list() function
  //String s = "Logitech QuickCam Zoom-WDM";
  //camera = new Capture(this, s, width, height, 30);

  // If no device is specified, will just use the default
  //camera = new Capture(this, 320, 240, 12);

  // Opens the settings page for this capture device
  //camera.settings();
}

void captureEvent(Capture camera)
{
 camera.read();
}

void draw()
{
 image(camera, 0, 0);
} 
