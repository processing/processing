PImage img;
img = loadImage("arch.jpg");
background(255);
tint(255, 51);
// Draw the image 10 times, moving each to the right
for (int i = 0; i < 10; i++) {
  image(img, i*10, 0);
}