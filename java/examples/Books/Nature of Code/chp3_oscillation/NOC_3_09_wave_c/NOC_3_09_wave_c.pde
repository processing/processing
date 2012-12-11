// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

float startAngle = 0;
float angleVel = 0.4;

void setup() {
  size(250,200);
  smooth();
}

void draw() {
  background(255);

  startAngle += 0.015;
  float angle = startAngle;

 for (int x = 0; x <= width; x += 24) {
    float y = map(sin(angle),-1,1,0,height);
    stroke(0);
    fill(0,50);
    strokeWeight(2);
    ellipse(x,y,48,48);
    angle += angleVel;
  } 

}



