/** 
 * Radial Pocky
 * by Ben Fry. 
 *
 * GSVideo version by Andres Colubri.  
 * 
 * Unwrap each frame of live video into a single line of pixels along a circle
 */ 
  
import codeanticode.gsvideo.*;


GSCapture video;
int videoCount;
int currentAngle;
int pixelCount;
int angleCount = 200;  // how many divisions

int radii[];
int angles[];


void setup() {
  // size must be set to video.width*video.height*2 in both directions
  size(600, 600); 

  // Uses the default video input, see the reference if this causes an error
  video = new GSCapture(this, 160, 120);
  video.start();
  videoCount = video.width * video.height;

  pixelCount = width*height;
  int centerX = width / 2;
  int centerY = height / 2;
  radii = new int[pixelCount];
  angles = new int[pixelCount];

  int offset = 0;
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      int dx = centerX - x;
      int dy = centerY - y;

      float angle = atan2(dy, dx);
      if (angle < 0) angle += TWO_PI;
      angles[offset] = (int) (angleCount * (angle / TWO_PI));

      int radius = (int) mag(dx, dy);
      if (radius >= videoCount) {
        radius = -1;
        angles[offset] = -1;
      }
      radii[offset] = radius;
      
      offset++;
    }
  }
  background(0);
}


void draw() {
  if (video.available()) {
    video.read();
    video.loadPixels();
  
    loadPixels();
    for (int i = 0; i < pixelCount; i++) {
      if (angles[i] == currentAngle) {
        pixels[i] = video.pixels[radii[i]];
      }
    }
    updatePixels();
    
    currentAngle++;
    if (currentAngle == angleCount) currentAngle = 0;
  }
}
