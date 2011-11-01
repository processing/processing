// If x is less than or equal to 100, then draw
// the rectangle. Otherwise, if x is greater than
// or equal to 300, draw the line. If x is between
// 100 and 300, draw the ellipse. Because x is 101,
// only the ellipse draws.
int x = 101;
if (x <= 100) {
  rect(33, 33, 34, 34);
} else if (x >= 300) {
  line(50, 0, 50, 100);
} else {
  ellipse(50, 50, 36, 36);
}