// Converts (x,y) coordinates into a position in the pixels[] array
loadPixels();
for (int y = 0; y < height; y++) {
  for (int x = 0; x < width; x++) {
    pixels[y*height + x] = color(102);
  }
}
updatePixels();