// The text expressions are "x > 100" and "x < 100"
// Because x is 150, the code inside the first block
// runs and the ellipse draws, but the code in the second
// block is not run and the rectangle is not drawn
int x = 150;
if (x > 100) { // If x is greater than 100,
  ellipse(50, 50, 36, 36); // draw this ellipse
}
if (x < 100) { // If x is less than 100
  rect(35, 35, 30, 30); // draw this rectangle
}
line(20, 20, 80, 80);