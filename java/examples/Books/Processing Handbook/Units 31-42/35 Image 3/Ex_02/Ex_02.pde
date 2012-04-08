smooth();
strokeWeight(8);
line(0, 0, width, height);
line(0, height, width, 0);
noStroke();
ellipse(18, 50, 16, 16);
PImage cross = get(); // Get the entire window
image(cross, 42, 30, 40, 40); // Resize to 40 x 40 pixels