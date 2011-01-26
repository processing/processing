// Shift the origin (0,0) to the lower-left corner
size(100, 100);
translate(0, height);
scale(1.0, -1.0);
line(0, 1, width, 1); // Draw x-axis
line(0, 1, 0, height ); // Draw y-axis
smooth();
noStroke();
fill(255, 204);
ellipse(0, 0, 45, 45); // Draw at the origin
ellipse(width/2, height/2, 45, 45);
ellipse(width, height, 45, 45);