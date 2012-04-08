int a = 10;
int b = 20;
// The expression "a > 5" must be true AND "b < 30"
// must be true. Because they are both true, the code
// in the block will run.
if ((a > 5) && (b < 30)) {
  line(20, 50, 80, 50);
}
// The expression "a > 15" is false, but "b < 30" is
// true. Because the AND operator requires both to be
// true, the code in the block will not run.
if ((a > 15) && (b < 30)) {
  ellipse(50, 50, 36, 36);
}