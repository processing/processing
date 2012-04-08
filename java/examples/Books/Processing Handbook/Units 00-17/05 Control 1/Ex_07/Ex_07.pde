// Because x is 190, only the ellipse draws
int x = 290;
if (x > 100) {              // If x is greater than 100,
  ellipse(50, 50, 36, 36);  // draw this ellipse.
} else {                    // Otherwise,
  rect(33, 33, 34, 34);     // draw this rectangle
}
line(20, 20, 80, 80);

