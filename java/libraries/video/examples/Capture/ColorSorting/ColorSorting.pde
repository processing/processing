/**
 * Color Sorting  
 * by Ben Fry. 
 *
 * Example that sorts all colors from the incoming video
 * and arranges them into vertical bars.
 */
 
 
import processing.video.*;

Capture video;
boolean cheatScreen;

Tuple[] captureColors;
Tuple[] drawColors;
int[] bright;

// How many pixels to skip in either direction
int increment = 5;

void setup() {
  size(800, 600);
  
  // This the default video input, see the GettingStartedCapture 
  // example if it creates an error
  video = new Capture(this, 160, 120);
  
  // Start capturing the images from the camera
  video.start();  
  
  int count = (video.width * video.height) / (increment * increment);
  bright = new int[count];
  captureColors = new Tuple[count];
  drawColors = new Tuple[count];
  for (int i = 0; i < count; i++) {
    captureColors[i] = new Tuple();
    drawColors[i] = new Tuple(0.5, 0.5, 0.5);
  }
}


void draw() {
  if (video.available()) {
    video.read();
    video.loadPixels();
    
    background(0);
    noStroke();

    int index = 0;
    for (int j = 0; j < video.height; j += increment) {
      for (int i = 0; i < video.width; i += increment) {
        int pixelColor = video.pixels[j*video.width + i];

        int r = (pixelColor >> 16) & 0xff;
        int g = (pixelColor >> 8) & 0xff;
        int b = pixelColor & 0xff;

        // Technically would be sqrt of the following, but no need to do
        // sqrt before comparing the elements since we're only ordering
        bright[index] = r*r + g*g + b*b;
        captureColors[index].set(r, g, b);

        index++;
      }
    }
    sort(index, bright, captureColors);

    beginShape(QUAD_STRIP);
    for (int i = 0; i < index; i++) {
      drawColors[i].target(captureColors[i], 0.1);
      drawColors[i].phil();

      float x = map(i, 0, index, 0, width);
      vertex(x, 0);
      vertex(x, height);
    }
    endShape();

    if (cheatScreen) {
      //image(video, 0, height - video.height);
      // Faster method of displaying pixels array on screen
      set(0, height - video.height, video);
    }
  }
}


void keyPressed() {
  if (key == 'g') {
    saveFrame();
  } else if (key == 'c') {
    cheatScreen = !cheatScreen;
  }
}


// Functions to handle sorting the color data


void sort(int length, int[] a, Tuple[] stuff) {
  sortSub(a, stuff, 0, length - 1);
}


void sortSwap(int[] a, Tuple[] stuff, int i, int j) {
  int T = a[i];
  a[i] = a[j];
  a[j] = T;

  Tuple v = stuff[i];
  stuff[i] = stuff[j];
  stuff[j] = v;
}


void sortSub(int[] a, Tuple[] stuff, int lo0, int hi0) {
  int lo = lo0;
  int hi = hi0;
  int mid;

  if (hi0 > lo0) {
    mid = a[(lo0 + hi0) / 2];

    while (lo <= hi) {
      while ((lo < hi0) && (a[lo] < mid)) {
        ++lo;
      }
      while ((hi > lo0) && (a[hi] > mid)) {
        --hi;
      }
      if (lo <= hi) {
        sortSwap(a, stuff, lo, hi);
        ++lo;
        --hi;
      }
    }

    if (lo0 < hi)
      sortSub(a, stuff, lo0, hi);

    if (lo < hi0)
      sortSub(a, stuff, lo, hi0);
  }
}
