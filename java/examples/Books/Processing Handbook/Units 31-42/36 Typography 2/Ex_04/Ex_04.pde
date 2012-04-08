PFont font;
int opacity = 0;
int direction = 1;

void setup() {
  size(100, 100);
  font = loadFont("EurekaSmallCaps-36.vlw");
  textFont(font);
}

void draw() {
  background(204);
  opacity += 2 * direction;
  if ((opacity < 0) || (opacity > 255)) {
    direction = -direction;
  }
  fill(0, opacity);
  text("fade", 4, 60);
}
