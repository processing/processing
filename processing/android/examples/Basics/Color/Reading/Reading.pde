/**
 * Reading. 
 * 
 * An image is recreated from its individual component colors.
 * The many colors of the image are created through modulating the 
 * red, green, and blue values. This is an exageration of an LCD display. 
 */

size(200, 200);
noStroke();
background(0);

// Load an image from the data directory
PImage img = loadImage("cait.jpg");
img.loadPixels();

// figure out how big to make each block based on 
// the sketch area and the size of the input image
int eachW = width / img.width;
int eachH = height / img.height;
int each = min(eachW, eachH);
// vertical stripes will be a third as wide
int stripeW = each / 3;
// make sure the block size is a multiple of 3
each = 3 * stripeW;

int left = (width - (img.width * each)) / 2;
int top = (height - (img.height * each)) / 2;

for (int y = 0; y < img.height; y++) {
  int y1 = top + y*each;
  
  for (int x = 0; x < img.width; x++) {
    int pixel = img.get(x, y);
    int x1 = left + x*each;
    
    fill(red(pixel), 0, 0);
    rect(x1 + stripeW*0, y1, stripeW, each);
    
    fill(0, green(pixel), 0);
    rect(x1 + stripeW*1, y1, stripeW, each);

    fill(0, 0, blue(pixel));
    rect(x1 + stripeW*2, y1, stripeW, each);
  }
}
