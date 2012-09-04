float angle = 0;
float aVelocity = 0;
float aAcceleration = 0.0001;

void setup() {
  size(800, 200);
  background(255);
  smooth();
}

void draw() {
  //background(255);
  rectMode(CORNER);
  noStroke();
  fill(255,5);
  rect(0,0,width,height);


  fill(127);
  stroke(0);

  translate(width/2, height/2);
  rectMode(CENTER);
  rotate(angle);
  stroke(0);
  strokeWeight(2);
  fill(127);
  line(-60, 0, 60, 0);
  ellipse(60, 0, 16, 16);
  ellipse(-60, 0, 16, 16);

  angle += aVelocity;
  aVelocity += aAcceleration;
}

