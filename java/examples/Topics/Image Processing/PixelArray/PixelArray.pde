/**
 * Pixel Array. 
 * 
 * Click and drag the mouse up and down to control the signal and 
 * press and hold any key to see the current pixel being read. 
 * This program sequentially reads the color of every pixel of an image
 * and displays this color to fill the window.  
 */
 
PImage img;
int direction = 1;
float signal;

void setup() {
  size(200, 200);
  noFill();
  stroke(255);
  frameRate(30);
  img = loadImage("ystone08.jpg");
}

void draw() {
  if (signal > img.width*img.height-1 || signal < 0) { 
    direction = direction * -1; 
  }

  if (mousePressed) {
    int mx = constrain(mouseX, 0, img.width-1);
    int my = constrain(mouseY, 0, img.height-1);
    signal = my*img.width + mx;
  } else {
    signal += 0.33*direction;
  }

  int sx = int(signal) % img.width;
  int sy = int(signal) / img.width;

  if (keyPressed) {
    set(0, 0, img);  // fast way to draw an image
    point(sx, sy);
    rect(sx - 5, sy - 5, 10, 10);
  } else {
    color c = img.get(sx, sy);
    background(c);
  }
}
