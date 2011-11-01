// Shift the origin (0,0) to the center
// and resizes the coordinate system
size(100, 100);
scale(width/2, height/2);
translate(1.0, 1.0);
strokeWeight(1.0/width);
line(-1, 0, 1, 0); // Draw x-axis
line(0, -1, 0, 1); // Draw y-axis
smooth();
noStroke();
fill(255, 204);
ellipse(0, 0, 0.9, 0.9); // Draw at the origin
ellipse(-1.0, 1.0, 0.9, 0.9);
ellipse(1.0, -1.0, 0.9, 0.9);