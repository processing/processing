PImage img;

void setup() {
  size(100, 100);
  smooth();
  frameRate(0.5);
  img = loadImage("palette10x10.jpg");
}

void draw() {
  background(0);
  for (int x = 0; x < img.width; x++) {
    for (int y = 0; y < img.height; y++) {
      float xpos1 = random(x * 10);
      float xpos2 = width - random(y * 10);
      color c = img.get(x, y);
      stroke(c);
      line(xpos1, 0, xpos2, height);
    }
  }
}
