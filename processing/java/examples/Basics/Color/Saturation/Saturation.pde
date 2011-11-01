/**
 * Saturation. 
 * 
 * Saturation is the strength or purity of the color and represents the 
 * amount of gray in proportion to the hue. A "saturated" color is pure 
 * and an "unsaturated" color has a large percentage of gray. 
 * Move the cursor vertically over each bar to alter its saturation. 
 */
 
int barWidth = 20;
int lastBar = -1;


void setup() {
  size(640, 360);
  colorMode(HSB, width, height, 100); 
  noStroke();
}


void draw() {
  int whichBar = mouseX / barWidth;
  if (whichBar != lastBar) {
    int barX = whichBar * barWidth;
    fill(barX, mouseY, 66);
    rect(barX, 0, barWidth, height);
    lastBar = whichBar;
  }
}
