/**
 * Blur. 
 * 
 * Bluring half of an image by processing it through a
 * low-pass filter. 
 */

float v = 1.0/9.0;
float[][] kernel = { { v, v, v },
                     { v, v, v },
                     { v, v, v } };

size(200, 200); 

PImage img = loadImage("trees.jpg"); // Load the original image
image(img, 0, 0); // Displays the image from point (0,0) 
img.loadPixels();

// Create an opaque image of the same size as the original
PImage edgeImg = createImage(img.width, img.height, RGB);

// Loop through every pixel in the image.
for (int y = 1; y < img.height-1; y++) { // Skip top and bottom edges
  for (int x = 1; x < img.width-1; x++) { // Skip left and right edges
    float sum = 0; // Kernel sum for this pixel
    for (int ky = -1; ky <= 1; ky++) {
      for (int kx = -1; kx <= 1; kx++) {
        // Calculate the adjacent pixel for this kernel point
        int pos = (y + ky)*img.width + (x + kx);
        // Image is grayscale, red/green/blue are identical
        float val = red(img.pixels[pos]);
        // Multiply adjacent pixels based on the kernel values
        sum += kernel[ky+1][kx+1] * val;
      }
    }
    // For this pixel in the new image, set the gray value
    // based on the sum from the kernel
    edgeImg.pixels[y*img.width + x] = color(sum);
  }
}
// State that there are changes to edgeImg.pixels[]
edgeImg.updatePixels();
image(edgeImg, 100, 0); // Draw the new image
