color c = color(204, 153, 102, 255);
float r = (c >> 16) & 0xFF; // Faster version of red(c)
float g = (c >> 8) & 0xFF; // Faster version of green(c)
float b = c & 0xFF; // Faster version of blue(c)
float a = (c >> 24) & 0xFF; // Faster version of alpha(c)
println(r + ", " + g + ", " + b + ", " + a);