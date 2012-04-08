PImage trees;

void setup() {
  size(100, 100);
  trees = loadImage("topangaCrop.jpg");
}

void draw() {
  int x = constrain(mouseX, 0, 50);
  set(x, 0, trees);
}
