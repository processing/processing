
// Based on code 15-09 (p. 131)


size(750, 2775);

float xnoise = 0.0;
float ynoise = 0.0;
float inc = 0.005;
for (int y = 0; y < height; y++) {
  for (int x = 0; x < width; x++) {
    float gray = noise(xnoise, ynoise) * 255;
    stroke(gray);
    point(x, y);
    xnoise = xnoise + inc;
  }
  xnoise = 0;
  ynoise = ynoise + inc;
}

//saveFrame("page_126.tif");
