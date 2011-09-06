/**
 * Histogram. 
 * 
 * Calculates the histogram of an image. 
 * A histogram is the frequency distribution 
 * of the gray levels with the number of pure black values
 * displayed on the left and number of pure white values on the right. 
 *
 * Note that this sketch will behave differently on Android, 
 * since most images will no longer be full 24-bit color.
 */
 
size(200, 200);

// Load an image from the data directory
// Load a different image by modifying the comments
PImage img = loadImage("cdi01_g.jpg");
image(img, 0, 0);
int[] hist = new int[256];

// Calculate the histogram
for (int i = 0; i < img.width; i++) {
  for (int j = 0; j < img.height; j++) {
    int bright = int(brightness(get(i, j)));
    hist[bright]++; 
  }
}

// Find the largest value in the histogram
int histMax = max(hist);

stroke(255);
// Draw half of the histogram (skip every second value)
for (int i = 0; i < img.width; i += 2) {
  // Map i (from 0..img.width-1) to a location in the histogram (0..255)
  int which = int(map(i, 0, img.width, 0, 255));
  // Convert the histogram value to a location between 
  // the bottom and the top of the picture
  int y = int(map(hist[which], 0, histMax, img.height, 0));
  line(i, img.height, i, y);
}
