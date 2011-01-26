strokeWeight(8);
line(0, 0, width, height);
line(0, height, width, 0);
PImage slice = get(0, 0, 20, 100); // Get window section
set(18, 0, slice);
set(50, 0, slice);