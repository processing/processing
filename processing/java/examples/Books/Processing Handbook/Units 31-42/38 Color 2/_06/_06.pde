color c = color(118, 22, 24); // Create a new color
int r1 = red(c); // ERROR! red() returns a float
float r2 = red(c); // Assign 118.0 to r2
int r3 = int(red(c)); // Assign 118 to r3