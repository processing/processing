/**
 * Animator.
 * 
 * Click and drag to draw and start the program.
 * 
 * A simple animation tool that displays a continuous cycle of
 * twenty-four images. Each image is displayed for 30 milliseconds 
 * to create animation. While each image is displayed, it’s possible 
 * to draw directly into it by pressing the mouse and moving the cursor. 
 * 
 */

int currentFrame = 0;
PImage[] frames = new PImage[24];
int lastTime = 0;

void setup() 
{
  size(640, 200);
  strokeWeight(12);
  smooth();
  background(204);
  for (int i = 0; i < frames.length; i++) {
    frames[i] = get(); // Create a blank frame
  }
}

void draw() 
{
  int currentTime = millis();
  if (currentTime > lastTime+30) {
    nextFrame();
    lastTime = currentTime;
  }
  if (mousePressed == true) {
    line(pmouseX, pmouseY, mouseX, mouseY);
  }
}

void nextFrame() 
{
  frames[currentFrame] = get(); // Get the display window
  currentFrame++; // Increment to next frame
  if (currentFrame >= frames.length) {
    currentFrame = 0;
  }
  image(frames[currentFrame], 0, 0);
}

