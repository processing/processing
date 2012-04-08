int[][] points = { {50, 18}, {61, 37}, {83, 43}, {69, 60},
                   {71, 82}, {50, 73}, {29, 82}, {31, 60},
                   {17, 43}, {39, 37} };

void setup() {
  size(100, 100);
  fill(0);
  smooth();
}

void draw() {
  background(204);
  translate(mouseX - 50, mouseY - 50);
  beginShape();
  for (int i = 0; i < points.length; i++) {
    vertex(points[i][0], points[i][1]);
  }
  endShape();
}
