PImage img;

void setup() {
  size(100, 100);
  noStroke();
  img = loadImage("palette10x10.jpg");
}

void draw() {
  int ix = int(random(img.width));
  int iy = int(random(img.height));
  color c = img.get(ix, iy);
  fill(c, 102);
  int xgrid = int(random(-2, 5)) * 25;
  int ygrid = int(random(-2, 5)) * 25;
  rect(xgrid, ygrid, 40, 40);
}
