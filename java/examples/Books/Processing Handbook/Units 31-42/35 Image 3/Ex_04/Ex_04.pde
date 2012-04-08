PImage trees;
trees = loadImage("topanga.jpg");
image(trees, 0, 0);
PImage crop = get(); // Get the entire window
image(crop, 0, 50); // Draw the image in a new position