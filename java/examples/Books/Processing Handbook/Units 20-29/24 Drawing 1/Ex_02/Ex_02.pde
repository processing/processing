// Draw from the previous mouse location to the current
// mouse location to create a continuous line
void setup() {
  size(100, 100);
}

void draw() {
  line(mouseX, mouseY, pmouseX, pmouseY);
}