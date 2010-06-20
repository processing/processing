int time1 = 2000;
int time2 = 4000;
float x = 0;

void setup() {
  size(480, 120);
  smooth();
}

void draw() {
  int currentTime = millis();
  background(204);
  if (currentTime > time2) {
    x -= 0.5;
  } else if (currentTime > time1) {
    x += 2;
  }
  ellipse(x, 60, 90, 90);
}

