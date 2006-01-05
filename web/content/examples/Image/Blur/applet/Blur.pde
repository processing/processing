// Blur
// by REAS <http://reas.com>

// Bluring half of an image by processing it through a
// low-pass filter

// Created 9 December 2002

size(200, 200); 
PImage a;  // Declare variable "a" of type PImage 
a = loadImage("trees.jpg"); // Load the images into the program 
image(a, 0, 0); // Displays the image from point (0,0) 

int n2 = 5/2;
int m2 = 5/2;
float  val = 1.0/25.0;
int[][] output = new int[width][height];
float[][] kernel = { {val, val, val, val, val},
                     {val, val, val, val, val},
                     {val, val, val, val, val},
                     {val, val, val, val, val},                                          
                     {val, val, val, val, val} };

// Convolve the image
for(int y=0; y<height; y++) {
  for(int x=0; x<width/2; x++) {
    float sum = 0;
    for(int k=-n2; k<=n2; k++) {
      for(int j=-m2; j<=m2; j++) {
        // Reflect x-j to not exceed array boundary
        int xp = x-j;
        int yp = y-k;
        if (xp < 0) {
          xp = xp + width;
        } else if (x-j >= width) {
          xp = xp - width;
        }
        // Reflect y-k to not exceed array boundary
        if (yp < 0) {
          yp = yp + height;
        } else if (yp >= height) {
          yp = yp - height;
        }
        sum = sum + kernel[j+m2][k+n2] * red(get(xp, yp)); 
      }
    }
    output[x][y] = int(sum);  
  }
}

// Display the result of the convolution
// by copying new data into the pixel buffer
loadPixels();
for(int i=0; i<height; i++) {
  for(int j=0; j<width/2; j++) {
    pixels[i*width + j] = color(output[j][i], output[j][i], output[j][i]);
  }
}
updatePixels();

