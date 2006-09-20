/**
 * ASCII Video
 * by Ben Fry. 
 * 
 * Text characters have been used to represent images since the earliest computers.
 * This sketch is a simple homage that re-interprets live video as ASCII text.
 * See the keyPressed function for more options, like changing the font size.
 */


import processing.opengl.*;
import processing.video.*;
 
Capture video;
int count;
boolean cheatScreen;

// all ascii characters, sorted according to their visual density
static final String vals =
  " .`-_':,;^=+/\"|)\\<>)iv%xclrs{*}I?!][1taeo7zjLu" +
  "nT#JCwfy325Fp6mqSghVd4EgXPGZbYkOA&8U$@KHDBWNMR0Q";

// how quickly to update each pixel (see below)
static final float STEP = 0.01;

// starting font size
float asciiFontSize = 1.5f;

float dgR[], dgG[], dgB[];

char values[];
float bright[];
char chars[];

PFont font;


public void setup() {
  // run at the default size
  size(640, 480, P3D);
  // or run full screen, more fun!
  //size(screen.width, screen.height, OPENGL);

  video = new Capture(this, 80, 60, 15);
  count = video.width * video.height;

  font = loadFont("UniversLTStd-Light-48.vlw");

  // for the 256 levels of brightness, distribute the letters across
  // the an array of 256 elements to use for the lookup
  values = new char[256];
  float multiplier = 256.0 / (float) vals.length();
  for (int i = 0; i < 256; i++) {
    int which = values[i] = vals.charAt((int) (i / multiplier));
  }

  // current characters for each position in the video
  chars = new char[count];

  // current brightness for each point
  bright = new float[count];
  for (int i = 0; i < count; i++) {
    // set each brightness at the midpoint to start
    bright[i] = 128.0f;
  }
}


public void captureEvent(Capture c) {
  c.read();
}


void draw() {
  background(0);

  pushMatrix();

  float hgap = (float) width / (float) video.width;
  float vgap = (float) height / (float) video.height;

  scale(max(hgap, vgap) * asciiFontSize);
  textFont(font, asciiFontSize);

  int index = 0;
  for (int y = 1; y < video.height; y++) {

    // move down for next line
    translate(0,  1.0 / asciiFontSize);

    pushMatrix();
    for (int x = 0; x < video.width; x++) {
      int pixelColor = video.pixels[index];
      int r = (pixelColor >> 16) & 0xff;
      int g = (pixelColor >> 8) & 0xff;
      int b = pixelColor & 0xff;

      // another option would be to properly calculate brightness as luminance:
      // luminance = 0.3*red + 0.59*green + 0.11*blue
      // or you could instead red + green + blue, and make the the values[] array
      // 256*3 elements long instead of just 256.
      int brightness = max(r, g, b);

      // the STEP variable is used to damp the changes so that letters flicker less
      bright[index] =
        (bright[index] * (1.0 - STEP) + (float)brightness * STEP);

      fill(pixelColor);
      text((char) values[(int) bright[index]], 0, 0);
      index++;

      // move over for next character
      translate(1.0 / asciiFontSize, 0);
    }
    popMatrix();
  }
  popMatrix();

  if (cheatScreen) {
    image(video, 0, height - video.height);
  }
}


/**
 * Handle key presses:
 * 'c' toggles the cheat screen that shows the original image in the corner
 * 'g' grabs an image and saves the frame to a tiff image
 * 'f' and 'F' increase and decrease the font size
 */
public void keyPressed() {
  switch (key) {
    case 'g': saveFrame(); break;
    case 'c': cheatScreen = !cheatScreen; break;
    case 'f': asciiFontSize *= 1.1f; break;
    case 'F': asciiFontSize *= 0.9f; break;
  }
}
