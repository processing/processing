int numFrames = 12; // The number of frames
PImage[] images = new PImage[numFrames]; // Make the array
int currentFrame = 1;

void setup() {
  size(240, 120);
  for (int i = 1; i < images.length; i++) {
    String imageName = "frame-" + nf(i, 4) + ".png";
    images[i] = loadImage(imageName); // Load each image
  }
  frameRate(24);
}

void draw() {
  image(images[currentFrame], 0, 0);
  currentFrame++; // Next frame
  if (currentFrame >= images.length) {
    currentFrame = 1;  // Return to first frame
  }
}

