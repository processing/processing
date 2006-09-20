/**
 * Disgrand
 * by Ben Fry. 
 * 
 * Example that sorts all colors from the incoming video
 * and arranges them into vertical bars.
 */
 
 
import processing.video.*;
import processing.opengl.*;

Capture video;
int count;
boolean cheatScreen = false;

float multiplier;
float dgR[], dgG[], dgB[];
Vec3f drgb[];
int value[];


public void setup() {
  size(800, 600, P3D);
  //size(screen.width, screen.height, OPENGL);

  noCursor();
  video = new Capture(this, 80, 60, 15);
  count = video.width * video.height;

  value = new int[count];
  drgb = new Vec3f[count];
  for (int i = 0; i < count; i++) {
    drgb[i] = new Vec3f();
  }
  dgR = new float[count];
  dgG = new float[count];
  dgB = new float[count];
  for (int i = 0; i < count; i++) {
    dgR[i] = 0.5;
    dgG[i] = 0.5;
    dgB[i] = 0.5;
  }
}


public void captureEvent(Capture c) {
  c.read();
}


public void draw() {
  background(0);
  colorMode(RGB, 1);
  noStroke();

  int index = 0;
  for (int j = 0; j < video.height; j += 5) {
    for (int i = 0; i < video.width; i += 5) {
      int pixelColor = video.pixels[j*video.width + i];

      int r = (pixelColor >> 16) & 0xff;
      int g = (pixelColor >> 8) & 0xff;
      int b = pixelColor & 0xff;

      value[index] = r*r + g*g + b*b;
      drgb[index].set(r, g, b);
      drgb[index].scale(1.0 / 255.0);

      index++;
    }
  }
  sort(index, value, drgb);

  multiplier = width / (float)index;
  beginShape(QUAD_STRIP);
  for (int i = 0; i < index; i++) {
    dgR[i] = dgR[i] * 0.9 + drgb[i].x * 0.1;
    dgG[i] = dgG[i] * 0.9 + drgb[i].y * 0.1;
    dgB[i] = dgB[i] * 0.9 + drgb[i].z * 0.1;
    fill(dgR[i], dgG[i], dgB[i]);

    float left = (float)i * multiplier;
    float right = ((float)(i+1)) * multiplier;

    vertex(right, 0);
    vertex(right, height);
  }
  endShape();

  if (cheatScreen) {
    image(video, 0, height - video.height);
  }
}


public void keyPressed() {
  switch (key) {
  case 'g': saveFrame(); break;
  case 'c': cheatScreen = !cheatScreen; break;
  }
}


/////////////////////////////////////////////////////////////

// functions to handle sorting the color data


void sort(int length, int a[], Vec3f stuff[]) {
  sortSub(a, stuff, 0, length - 1);
}


void sortSwap(int a[], Vec3f stuff[], int i, int j) {
  int T = a[i];
  a[i] = a[j];
  a[j] = T;

  Vec3f v = stuff[i];
  stuff[i] = stuff[j];
  stuff[j] = v;
}


void sortSub(int a[], Vec3f stuff[], int lo0, int hi0) {
  int lo = lo0;
  int hi = hi0;
  int mid;

  if (hi0 > lo0) {
    mid = a[(lo0 + hi0) / 2];

    while (lo <= hi) {
      while ((lo < hi0) && (a[lo] < mid))
        ++lo;

      while ((hi > lo0) && (a[hi] > mid))
        --hi;

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
