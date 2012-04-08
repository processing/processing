float power = 3; // Turbulence power
float d = 8; // Turbulence density
noStroke();
for (int y = 0; y < height; y++) {
  for (int x = 0; x < width; x++) {
    float total = 0.0;
    for (float i = d; i >= 1; i = i/2.0) {
      total += noise(x/d, y/d) * d;
    }
    float turbulence = 128.0 * total / d;
    float base = (x * 0.2) + (y * 0.12);
    float offset = base + (power * turbulence / 256.0);
    float gray = abs(sin(offset)) * 256.0;
    stroke(gray);
    point(x, y);
  }
}