/**
 * Recursion. 
 * 
 * A demonstration of recursion, which means functions call themselves. 
 * Notice how the drawCircle() function calls itself at the end of its block. 
 * It continues to do this until the variable "level" is equal to 1. 
 */
 
void setup() {
  size(640, 360);
  noStroke();
  smooth();
  drawCircle(width*0.5, height*0.5, 160, 8);
}

void drawCircle(float x, float y, int radius, int level) {          
  float tt = 126 * level/6.0;
  fill(tt, 153);
  ellipse(x, y, radius*2, radius*2);      
  if(level > 1) {
    level = level - 1;
    int num = int(random(2, 6));
    for(int i=0; i<num; i++) {
      float a = random(0, TWO_PI);
      float nx = x + cos(a) * 6.0 * level;
      float ny = y + sin(a) * 6.0 * level;
      drawCircle(nx, ny, radius/2, level);
    }
  }
}
