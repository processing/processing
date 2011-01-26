
// Based on code 33-14 (p. 307)


float inc = 0.0;

boolean record = false;

int numLines = 170;
float[] y;
float[] x;

float mx;
float my;

void setup() {
  size(180, 666);
  smooth();
  noFill();
  strokeWeight(0.25);  
  y = new float[height];
  x = new float[height];

}

void draw() {

  if(record) {
    beginRecord(PDF, "page_300.pdf");
  }

  background(255);

  // Shift the values to the right
  for (int i = y.length-1; i > 0; i--) { 
    y[i] = y[i-1];
  } 
  // Add new values to the beginning
  my += (mouseX-my) * 0.1;
  y[0] = my;

  beginShape();
  for (int i = 1; i < y.length; i++) {
    vertex(y[i], i);
  }
  endShape();

  // Shift the values to the right
  for (int i = x.length-1; i > 0; i--) { 
    x[i] = x[i-1];
  } 
  // Add new values to the beginning
  mx += (mouseY-mx) * 0.1;
  x[0] = mx;


  beginShape();
  for (int i = 1; i < x.length; i++) {
    vertex(x[i] * float(width)/height, i);
  }
  endShape();

  if(record) {
    endRecord();
    record = false; 
  }

}

void mousePressed() {
  record = true;
}



