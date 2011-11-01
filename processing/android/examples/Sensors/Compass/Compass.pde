CompassManager compass;
float direction;


void setup() {
  compass = new CompassManager(this);
}


void pause() {
  if (compass != null) compass.pause();
}


void resume() {
  if (compass != null) compass.resume();
}


void draw() {
  background(255);
  fill(192, 0, 0);
  noStroke();
  
  translate(width/2, height/2);
  scale(2);
  rotate(direction);
  beginShape();
  vertex(0, -50);
  vertex(-20, 60);
  vertex(0, 50);
  vertex(20, 60);
  endShape(CLOSE);
}


void directionEvent(float newDirection) {
  direction = newDirection;
}
