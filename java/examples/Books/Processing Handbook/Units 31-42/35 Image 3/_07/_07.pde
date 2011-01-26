PImage trees;
int y = 0;

void setup() {
  size(100, 100);
  trees = loadImage("topangaCrop.jpg");
}

void draw() {
  image(trees, 0, 0);
  y = constrain(mouseY, 0, 99);
  for (int i = 0; i < 49; i++) {
    color c = get(i, y);
    stroke(c);
    line(i + 50, 0, i + 50, 100);
  }
  stroke(255);
  line(0, y, 49, y);
}
