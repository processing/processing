// Draw with an image that has transparency
PImage alphaImg;

void setup() {
  size(100, 100);
  // This image is partially transparent
  alphaImg = loadImage("alphaArch.png");
}

void draw() {
  int ix = mouseX - alphaImg.width/2;
  int iy = mouseY - alphaImg.height/2;
  image(alphaImg, ix, iy);
}