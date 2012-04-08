int[] x = {50, 61, 83, 69, 71, 50, 29, 31, 17, 39};
int[] y = { 18, 37, 43, 60, 82, 73, 82, 60, 43, 37 };

beginShape();
// Reads one array element every time through the for()
for (int i = 0; i < x.length; i++) {
  vertex(x[i], y[i]);
}
endShape(CLOSE);
