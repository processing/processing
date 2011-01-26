// Draw with an image sliver
PImage lineImage;

void setup() {
  size(100, 100);
  // This image is 100 pixels wide, but one pixel tall
  lineImage = loadImage("imageline.jpg");
}

void draw() {
  image(lineImage, mouseX-lineImage.width/2, mouseY);
}