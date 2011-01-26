
// Based on the Cursor example on page 257


import processing.pdf.*;

boolean record = false;

int gx, gy;
int mode, nextmode;
int nummodes; 
boolean forapplet = false;

float mx, my, lastmx, lastmy;
float lastrot, lastsc;

float bgx, bgy;

float p_x, p_y;
float p_fx, p_fy;
float p_v2, p_vx, p_vy;
float p_a2, p_ax, p_ay;
float p_mass, p_drag;

void setup() {
  size(442, 550);
  gx = width; 
  gy = height;
  colorMode(RGB, 1.0);
  strokeWeight(0.5);
  strokeJoin(ROUND);

  nummodes = 4;
  mode = 4;

  bgx = 0;
  bgy = 0;
  mx = gx/2;
  my = gy/2;
}


void draw() {
  
  if(record) {
    beginRecord(PDF, "page_254.pdf");     // Start writing to PDF 
  }
  
  lastmx = mx;
  lastmy = my;

  mx = mouseX;
  my = mouseY;

  background(1.0);

  // Grid
  fill(1,1,1);
  stroke(.2,.2,.2);

  float rot;
  for (int i=16; i<gx+2; i+=32) {
    for (int j=11; j<gy; j+=30) {
      rot = -PI/4. + atan2(j-my, i-mx);
      drawCursor(i, j, .5, rot);
    }
  }  

  if(record) {
    endRecord();
    record = false; 
  }  
  
}

void drawCursor(float x, float y, float myscale, float myrot) {
  // Draw generic arrow cursor
  if (forapplet) y -= gy/2;
  pushMatrix();
  translate(x, y);
  rotate(myrot);
  scale(myscale, myscale);
  beginShape(POLYGON);
  vertex(7, 21);
  vertex(4, 13);
  vertex(1, 16);
  vertex(0, 16);
  vertex(0, 0);  // Tip of cursor shape
  vertex(1, 0);
  vertex(12, 11);
  vertex(12, 12);
  vertex(7, 12);
  vertex(10, 20);
  vertex(9, 21);
  vertex(7, 21);
  endShape();  
  popMatrix();
}


void keyPressed() {
  record = true;
}





