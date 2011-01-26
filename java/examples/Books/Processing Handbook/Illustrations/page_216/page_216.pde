
// Based on code 24-08 (p. 221)


PImage lineImage;
int x;

void setup() {
  size(750/2, 2292/2);
  background(255);
  lineImage = loadImage("paris-line.jpg");
}

void draw() {
  if(mousePressed) {
    x = mouseX;
  }
  
  image(lineImage, x-lineImage.width/2, mouseY);
  image(lineImage, x-lineImage.width/2, height-mouseY);
}

void keyPressed() {
  saveFrame("page-216-####.tif"); 
}
