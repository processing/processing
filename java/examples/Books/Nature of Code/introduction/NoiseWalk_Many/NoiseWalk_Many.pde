// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

Walker[] w;

int total = 0;

void setup() {
  size(600, 400);

  w = new Walker[10];
  for (int i = 0; i < w.length; i++) {
    w[i] = new Walker();
  }
}

void draw() {
  background(255);
  int o = int(map(mouseX,0,width,1,8));
  noiseDetail(o,0.3);

  if (frameCount % 30 == 0) {
    total = total + 1;
    if (total > w.length-1) {
      total = w.length-1;
    }
  }

  for (int i = 0; i < total; i++) {
    w[i].walk();
    w[i].display();
  }
}

