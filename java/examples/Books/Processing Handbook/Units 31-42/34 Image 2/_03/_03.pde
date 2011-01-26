int numFrames = 5; // The number of animation frames
PImage[] images = new PImage[numFrames];

void setup() {
  size(100, 100);
  for (int i = 0; i < images.length; i++) {
    String imageName = "ani-" + nf(i, 3) + ".gif";
    images[i] = loadImage(imageName);
  }
}

void draw() {
  int frame = int(random(0, numFrames)); // The frame to display
  image(images[frame], 0, 0);
  frameRate(random(1, 60.0));
}
