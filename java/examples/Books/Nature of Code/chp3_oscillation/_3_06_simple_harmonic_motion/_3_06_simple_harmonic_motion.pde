float angle = 0;
float aVelocity = 0.03;

void setup() {
  size(640,360);

  smooth();
}

void draw() {
  background(255);
  
  float amplitude = 300;
  float x = amplitude * cos(angle);
  angle += aVelocity;
  
  ellipseMode(CENTER);
  stroke(0);
  fill(175);
  translate(width/2,height/2);
  line(0,0,x,0);
  ellipse(x,0,20,20);
}
