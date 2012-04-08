PImage arch = loadImage("arch.jpg");
background(arch);
loadPixels();
for (int i = 0; i < width*height; i++) {
  color p = pixels[i]; // Grab pixel
  float r = 255 - red(p); // Modify red value
  float g = 255 - green(p); // Modify green value
  float b = 255 - blue(p); // Modify blue value
  pixels[i] = color(r, g, b); // Assign modified value
}
updatePixels();
