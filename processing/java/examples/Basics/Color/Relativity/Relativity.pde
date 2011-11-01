/**
 * Relativity. 
 * 
 * Each color is perceived in relation to other colors. The top and bottom 
 * bars each contain the same component colors, but a different display order 
 * causes individual colors to appear differently. 
 */
 
color a, b, c, d, e;

void setup() {
  size(640, 360);
  noStroke();
  a = color(165, 167, 20);
  b = color(77, 86, 59);
  c = color(42, 106, 105);
  d = color(165, 89, 20);
  e = color(146, 150, 127);
  noLoop();  // Draw only one time
}

void draw() {
  drawBand(a, b, c, d, e, 0, width/128);
  drawBand(c, a, d, b, e, height/2, width/128);
}

void drawBand(color v, color w, color x, color y, color z, int ypos, int barWidth) {
  int num = 5;
  color[] colorOrder = { v, w, x, y, z };
  for(int i = 0; i < width; i += barWidth*num) {
    for(int j = 0; j < num; j++) {
      fill(colorOrder[j]);
      rect(i+j*barWidth, ypos, barWidth, height/2);
    }
  }
}






