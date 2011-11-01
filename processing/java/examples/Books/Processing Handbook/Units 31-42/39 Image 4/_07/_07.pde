PImage img = loadImage("forest.jpg");
PImage img2 = loadImage("airport.jpg");
img.blend(img2, 12, 12, 76, 76, 12, 12, 76, 76, ADD);
image(img, 0, 0);