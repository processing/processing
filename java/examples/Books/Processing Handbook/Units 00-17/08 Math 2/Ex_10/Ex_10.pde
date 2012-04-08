// Draw circles at points along the curve y = x^4
noFill();
smooth();
for (int x = 0; x < 100; x += 5) {
  float n = norm(x, 0.0, 100.0); // Range 0.0 to 1.0
  float y = pow(n, 4); // Calculate curve
  y *= 100; // Scale y to range 0.0 to 100.0
  strokeWeight(n * 5); // Increase thickness
  ellipse(x, y, 120, 120);
}