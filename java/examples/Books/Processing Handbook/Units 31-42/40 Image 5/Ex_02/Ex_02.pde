// These 3 lines of code is equivalent to: set(25, 50, color(0))
loadPixels();
pixels[50*width + 25] = color(0);
updatePixels();