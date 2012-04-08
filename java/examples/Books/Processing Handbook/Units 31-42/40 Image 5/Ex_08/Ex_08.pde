PImage arch = loadImage("arch.jpg");
int count = arch.width * arch.height;
arch.loadPixels();
loadPixels();
for (int i = 0; i < count; i += 3) {
  pixels[i] = arch.pixels[i];
}
updatePixels();
