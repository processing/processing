void setup() {
  size(100, 100);
}

void draw() {
  background(204);
  line(0, 0, mouseX, height);
  line(width, 0, 0, mouseY);
}
void mousePressed() {
  save("line.tif");
}
