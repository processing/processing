// Avoids (x,y) coordinates
int wh = width*height;
loadPixels();
for (int index = 0; index < wh; index++) {
  pixels[index] = color(102);
}
updatePixels();