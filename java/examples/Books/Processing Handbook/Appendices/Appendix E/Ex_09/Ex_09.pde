// Only calculate the color once
int wh = width*height;
color c = color(102);
loadPixels();
for (int index = 0; index < wh; index++) {
  pixels[index] = c;
}
updatePixels();