import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Animator extends PApplet {/**
 * Animator.
 * 
 * A simple animation tool that displays a continuous cycle of
 * twelve images. Each image is displayed for 100 milliseconds 
 * (one tenth of a second) to create animation. While each image 
 * is displayed, it\u2019s possible to draw directly into it by
 * pressing the mouse and moving the cursor. Start drawing to get
 * it started.
 * 
 * Created 27 May 2007
 */

int currentFrame = 0;
PImage[] frames = new PImage[12];
int lastTime = 0;

public void setup() 
{
  size(200, 200);
  strokeWeight(4);
  smooth();
  background(204);
  for (int i = 0; i < frames.length; i++) {
    frames[i] = get(); // Create a blank frame
  }
}

public void draw() 
{
  int currentTime = millis();
  if (currentTime > lastTime+100) {
    nextFrame();
    lastTime = currentTime;
  }
  if (mousePressed == true) {
    line(pmouseX, pmouseY, mouseX, mouseY);
  }
}

public void nextFrame() 
{
  frames[currentFrame] = get(); // Get the display window
  currentFrame++; // Increment to next frame
  if (currentFrame >= frames.length) {
    currentFrame = 0;
  }
  image(frames[currentFrame], 0, 0);
}
static public void main(String args[]) {   PApplet.main(new String[] { "Animator" });}}