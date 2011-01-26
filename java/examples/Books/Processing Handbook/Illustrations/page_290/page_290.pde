
// Based on code 32-07 (p. 297)


float inc = 0.0;

boolean record = false;

void setup() {
  size(513, 462);
  smooth();
}

void draw() {
  
  if(record) {
    beginRecord(PDF, "page_290.pdf");
  }
  
  stroke(0, 143);
  randomSeed(0);
  background(255);
  inc += 0.01;
  float angle = sin(inc)/10.0 + sin(inc*1.2)/20.0;
  //tail(18, 9, angle/1.3);
  //tail(33, 12, angle);
  //tail(44, 10, angle/1.3);
  //tail(62, 5, angle);
  //tail(88, 7, angle*2);
  for(int i=-20; i<width+20; i+=20) {
    tail(i, 100, int(random(5, 12)), angle*random(1.0, 1.3));
    tail(i, 200, int(random(5, 12)), -angle*random(1.0, 1.3));
    tail(i, 300, int(random(5, 12)), angle*random(1.0, 1.3));
    tail(i, 400, int(random(5, 12)), -angle*random(1.0, 1.3));
  }
  
  if(record) {
    endRecord();
    record = false; 
  }
  
}

void mousePressed() {
  record = true;
}


void tail(int x, int y, int units, float angle) {
  pushMatrix();
  translate(x, y);
  for (int i = units; i > 0; i--) {
    strokeWeight(i/2.0);
    line(0, 0, 0, -8);
    translate(0.0, -8);
    rotate(angle);
  }
  popMatrix();
}
