// Replaces the multiplication y*height with an addition
int offset = 0;
loadPixels();
for (int y = 0; y < height; y++) {
  for (int x = 0; x < width; x++) {
    pixels[offset + x] = color(102);
  }
  offset += width; // Avoids the multiply
}
updatePixels();