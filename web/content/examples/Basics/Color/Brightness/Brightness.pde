/**
 * Brightness 
 * by Rusty Robison. 
 * 
 * Brightness is the relative lightness or darkness of a color.
 * Move the cursor vertically over each bar to alter its brightness. 
 * 
 * Updated 28 February 2010.
 */
 
int barWidth = 5;
int lastBar = -1;

void setup() {
  size(200, 200);
  colorMode(HSB, 360, 100, height);
  noStroke();
  background(0);
}

void draw() {
  int whichBar = mouseX / barWidth;
  if (whichBar != lastBar) {
    int barX = whichBar * barWidth;
    fill(barX, 100, mouseY);
    rect(barX, 0, barWidth, height);
    lastBar = whichBar;
  }
}
