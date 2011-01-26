int numFrames = 12; // The number of animation frames
int frame = 0; // The frame to display
PImage[] images = new PImage[numFrames]; // Image array

void setup() {
  size(100, 100);
  frameRate(30); // Maximum 30 frames per second
  images[0] = loadImage("ani-000.gif");
  images[1] = loadImage("ani-001.gif");
  images[2] = loadImage("ani-002.gif");
  images[3] = loadImage("ani-003.gif");
  images[4] = loadImage("ani-004.gif");
  images[5] = loadImage("ani-005.gif");
  images[6] = loadImage("ani-006.gif");
  images[7] = loadImage("ani-007.gif");
  images[8] = loadImage("ani-008.gif");
  images[9] = loadImage("ani-009.gif");
  images[10] = loadImage("ani-010.gif");
  images[11] = loadImage("ani-011.gif");
}

void draw() {
  frame++;
  if (frame == numFrames) {
    frame = 0;
  }
  image(images[frame], 0, 0);
}
