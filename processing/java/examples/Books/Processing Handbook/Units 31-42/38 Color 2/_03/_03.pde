colorMode(HSB, 360, 100, 100); // Set color mode to HSB
color c = color(210, 100, 40); // Create a new color
float h = hue(c); // Assign 210.0 to h
float s = saturation(c); // Assign 100.0 to s
float b = brightness(c); // Assign 40.0 to b
println(h + ", " + s + ", " + b); // Prints "210.0, 100.0, 40.0"