color g1 = color(102); // Middle gray
color g2 = color(51); // Dark gray
color g3 = blendColor(g1, g2, MULTIPLY); // Create black
noStroke();
fill(g1);
rect(50, 0, 50, 100); // Right rect
fill(g2);
rect(20, 25, 30, 50); // Left rect
fill(g3);
rect(50, 25, 20, 50); // Overlay rect