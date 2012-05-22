
PShape s;

void setup() {
  size(640, 360, P2D);
  smooth();
  s = createShape();
  s.noFill();
  s.stroke(0);
  s.beginContour();
  for (float a = -PI; a < 0; a += 0.1) {
    float r = random(45, 55);
    s.vertex(-60+r*cos(a), r*sin(a));
  }
  s.endContour();

  s.beginContour();
  for (float a = 0; a < PI; a += 0.1) {
    float r = random(45, 55);
    s.vertex(60+r*cos(a), r*sin(a));
  }
  s.endContour();
  s.end();
}

void draw() {
  background(255);
  translate(width/2, height/2);
  s.rotate(0.01);
  shape(s);
}

