colorMode(RGB, 1.0); // Sets color mode to HSB
color c = color(0.2, 0.8, 1.0); // Creates a new color
float r = red(c); // Assign 0.2 to r
float h = hue(c); // Assign 0.5416667 to h
println(r + ", " + h); // Prints "0.2, 0.5416667"