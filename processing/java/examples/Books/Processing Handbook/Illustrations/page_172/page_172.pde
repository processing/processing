
// Based on code 21-14 (p. 192)


void setup() {
  size(200, 666);
  //size(200, 666, PDF, "page_172.pdf");
  background(255);
  smooth();
  noStroke();
  noLoop();
  fill(100);
  randomSeed(0);
  strokeWeight(0.25);
}

void draw() {
  for(int i=0; i<12; i++) {
    vine(10+int(random(width-20)), int(random(10, 50)), int(random(2, 6)));
  }
  //exit();
}

void vine(int x, int numLeaves, int leafSize ) {
  stroke(0);
  line(x, 0, x, height);
  noStroke();
  int gap = (height)/numLeaves;
  int direction = 1;
  for (int i = 0; i < numLeaves; i++) {
    int r = int(random(gap));
    leaf( x, gap*i + r, leafSize, direction);
    direction = direction * -1; 
  }
}

void leaf(int x, int y, int size, int d) {
  pushMatrix();
  translate(x, y); // Move to position
  scale(size); // Scale to size
  beginShape(); // Draw the shape
  vertex(1.0*d, -0.7);
  bezierVertex(1.0*d, -0.7, 0.4*d, -1.0, 0.0, 0.0);
  bezierVertex(0.0, 0.0, 1.0*d, 0.4, 1.0*d, -0.7);
  endShape();
  popMatrix();
}

