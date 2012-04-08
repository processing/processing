background(255);
PImage img = loadImage("airport.jpg");
PImage maskImg = loadImage("airportmask.jpg");
img.mask(maskImg);
image(img, 0, 0);