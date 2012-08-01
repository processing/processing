int numFrames = 12; // The number of animation frames
PImage[] images = new PImage[numFrames]; // Image array

void setup() {
  size(100, 100);
  frameRate(30); // Maximum 30 frames per second

  // Automate the image loading procedure. Numbers less than 100
  // need an extra zero added to fit the names of the files.
  for (int i = 0; i < images.length; i++) {
    // Construct the name of the image to load
    String imageName = "ani-" + nf(i, 3) + ".gif";
    images[i] = loadImage(imageName);
  }
}

void draw() {
  // Calculate the frame to display, use % to cycle through frames
  int frame = frameCount % numFrames;
  image(images[frame], 0, 0);
}
