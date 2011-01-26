// Shift the origin (0,0) to the center
size(100, 100);
translate(width/2, height/2);
line(-width/2, 0, width/2, 0); // Draw x-axis
line(0, -height/2, 0, height/2); // Draw y-axis
smooth();
noStroke();
fill(255, 204);
ellipse(0, 0, 45, 45); // Draw at the origin
ellipse(-width/2, height/2, 45, 45);
ellipse(width/2, -height/2, 45, 45);