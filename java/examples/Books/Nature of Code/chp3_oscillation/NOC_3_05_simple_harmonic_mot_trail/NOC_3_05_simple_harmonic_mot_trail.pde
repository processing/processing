void setup() {
  size(800,200);
  background(255);
}

void draw() {
  noStroke();
  fill(255,5);
  rect(0,0,width,height);
  float period = 120;
  float amplitude = 300;
  // Calculating horizontal location according to formula for simple harmonic motion
  float x = amplitude * cos(TWO_PI * frameCount / period);  
  stroke(0);
  strokeWeight(2);
  fill(127);
  translate(width/2,height/2);
  line(0,0,x,0);
  ellipse(x,0,48,48);
}
