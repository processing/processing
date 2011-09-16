/**
 * Zoom. 
 * 
 * Move the cursor over the image to alter its position. Click and press
 * the mouse to zoom. This program displays a series of lines with their 
 * heights corresponding to a color value read from an image. 
 */

// @pjs preload must be used to preload media if the program is 
// running with Processing.js
/* @pjs preload="ystone08.jpg"; */ 

PImage img;
int[][] imgPixels;
float sval = 1.0;
float nmx, nmy;
int res = 5;

void setup() {
  size(640, 360, P3D);
  noFill();
  stroke(255);
  img = loadImage("ystone08.jpg");
  imgPixels = new int[img.width][img.height];
  for (int i = 0; i < img.height; i++) {
    for (int j = 0; j < img.width; j++) {
      imgPixels[j][i] = img.get(j, i);
    }
  }
}

void draw() {
  background(0);

  nmx += (mouseX-nmx)/20; 
  nmy += (mouseY-nmy)/20; 

  if(mousePressed) { 
    sval += 0.005; 
  } 
  else {
    sval -= 0.01; 
  }

  sval = constrain(sval, 1.0, 2.0);

  translate(width/2 + nmx * sval-100, height/2 + nmy*sval - 100, -50);
  scale(sval);
  rotateZ(PI/9 - sval + 1.0);
  rotateX(PI/sval/8 - 0.125);
  rotateY(sval/8 - 0.125);

  translate(-width/2, -height/2, 0);

  for (int i = 0; i < img.height; i += res) {
    for (int j = 0; j < img.width; j += res) {
      float rr = red(imgPixels[j][i]); 
      float gg = green(imgPixels[j][i]);
      float bb = blue(imgPixels[j][i]);
      float tt = rr+gg+bb;
      stroke(rr, gg, gg);
      line(i, j, tt/10-20, i, j, tt/10 );
    }
  }
}






