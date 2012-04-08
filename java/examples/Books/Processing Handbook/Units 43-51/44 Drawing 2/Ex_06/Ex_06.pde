int currentFrame = 0;
PImage[] frames = new PImage[12];
int lastTime = 0;

void setup() {
  size(100, 100);
  strokeWeight(4);
  smooth();
  background(204);
  for (int i = 0; i < frames.length; i++) {
    frames[i] = get(); // Create a blank frame
  }
}

void draw() {
  int currentTime = millis();
  if (currentTime > lastTime + 100) {
    nextFrame();
    lastTime = currentTime;
  }
  if (mousePressed == true) {
    line(pmouseX, pmouseY, mouseX, mouseY);
  }
}

void nextFrame() {
  frames[currentFrame] = get(); // Get the display window
  currentFrame++; // Increment to next frame
  if (currentFrame >= frames.length) {
    currentFrame = 0;
  }
  image(frames[currentFrame], 0, 0);
}
