// Save 24 frames, from x-1000.tif to x-1023.tif
void draw() {
  background(204);
  line(mouseX, mouseY, pmouseX, pmouseY);
  if ((frameCount > 99) && (frameCount < 1024)) {
    saveFrame("x-####.tif");
  }
}
