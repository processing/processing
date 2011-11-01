PImage trees;
trees = loadImage("topanga.jpg");
noStroke();
image(trees, 0, 0);
color c = get(20, 30); // Get color at (20, 30)
fill(c);
rect(20, 30, 40, 40);