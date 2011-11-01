
// Based on code 8-08 (p. 83)


size(360, 550);
//size(360, 550, PDF, "page_078.pdf");
background(255);
stroke(0);
noFill();

float ax = 0.0;
float ay = 0.0;

for (float e = 2; e < 36; e += 1.0) {
  for (int x = 0; x < width; x += 2) {
    float n = norm(x, 0.0, width);  // Range 0.0 to 1.0
    float y = 1 - pow(n, e);        // Calculate curve
    y *= height;     // Range 0.0 to height
    if (x > 0) {
      line(ax, ay, x, y);
    } 
    ax = x;
    ay = y;
  }
}



