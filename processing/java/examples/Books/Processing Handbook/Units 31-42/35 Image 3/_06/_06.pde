PImage trees;

void setup() {
  size(100, 100);
  noStroke();
  trees = loadImage("topangaCrop.jpg");
}

void draw() {
  image(trees, 0, 0);
  color c = get(mouseX, mouseY);
  fill(c);
  rect(50, 0, 50, 100);
}
