// If x is greater than 100 and less than 300, draw the
// ellipse. If x is greater than or equal to 300, draw
// the line. If x is not greater than 100, draw the
// rectangle. Because x is 420, only the line draws.
int x = 420;
if (x > 100) { // First test to draw ellipse or line
  if (x < 300) { // Second test determines which to draw
   ellipse(50, 50, 36, 36);
  } else {
   line(50, 0, 50, 100);
  }
} else {
   rect(33, 33, 34, 34);
}