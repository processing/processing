
// Based on code 22-11 (p. 204)


import processing.pdf.*;

int x;        // X-coordinate
int y;        // Y-coordinate
int r = 350;  // Starting radius
int n = 8;    // Number of recursions
int rs = 12;  // Random seed value

void setup() { 
  size(250, 925);
  //size(250, 925, PDF, "page_xx.pdf"); 
  noStroke(); 
  smooth(); 
  randomSeed(rs);
  y = int (height * 0.33);
  x = width/2;
  noLoop();
} 

void draw() {
  background(255);
  drawCircle(x, y, r, n); 
  exit();
}

void drawCircle(float x, float y, int radius, int num) {                    
  float value = 126 * num / 6.0; 
  fill(value, 153); 
  ellipse(x, y, radius*2, radius*2);      
  if (num > 1) { 
    num = num - 1; 
    int branches = int(random(2, 6)); 
    for (int i = 0; i < branches; i++) { 
      float a = random(0, TWO_PI); 
      float newx = x + cos(a) * 6.0 * num; 
      float newy = y + sin(a) * 6.0 * num; 
      drawCircle(newx, newy+26, radius/2, num); 
    } 
  } 
}
