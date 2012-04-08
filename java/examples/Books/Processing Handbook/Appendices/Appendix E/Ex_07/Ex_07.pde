// Avoid the calculation y*height+width
int index = 0;
loadPixels();
for (int y = 0; y < height; y++) {
  for (int x = 0; x < width; x++) {
    pixels[index++] = color(102);
  }
}
updatePixels();