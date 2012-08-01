PImage img;
img = loadImage("arch.jpg");
background(255);
tint(255, 102); // Alpha to 102 without changing the tint
image(img, 0, 0, 100, 100);
tint(255, 204, 0, 153); // Tint to yellow, alpha to 153
image(img, 20, 20, 100, 100);