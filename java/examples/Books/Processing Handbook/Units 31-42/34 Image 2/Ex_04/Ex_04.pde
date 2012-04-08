int numFrames = 12; // The number of animation frames
int topFrame = 0; // The top frame to display
int bottomFrame = 0; // The bottom frame to display
PImage[] images = new PImage[numFrames];
int lastTime = 0;

void setup() {
  size(100, 100);
  frameRate(30);
  for (int i = 0; i < images.length; i++) {
    String imageName = "ani-" + nf(i, 3) + ".gif";
    images[i] = loadImage(imageName);
  }
}

void draw() {
  topFrame = (topFrame + 1) % numFrames;
  image(images[topFrame], 0, 0);
  if ((millis() - lastTime) > 500) {
    bottomFrame = (bottomFrame + 1) % numFrames;
    lastTime = millis();
  }
  image(images[bottomFrame], 0, 50);
}
