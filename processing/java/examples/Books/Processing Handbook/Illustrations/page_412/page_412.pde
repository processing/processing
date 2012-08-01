
// Based on code 44-01 (p. 414)


void setup() {
  size(400, 666);
  smooth();
}

void draw() {
  if(mouseX != 0 && mouseY != 0 && pmouseX !=0 && pmouseY != 0) {
    float s = dist(mouseX, mouseY, pmouseX, pmouseY) + 1;
    noStroke();
    fill(0, 102);
    ellipse(mouseX, mouseY, s, s);
    stroke(255);
    point(mouseX, mouseY);
  }
}

void mousePressed() {
  beginRecord(PDF, "page_412.pdf");
  background(255); 
}

void mouseReleased() {
  endRecord(); 
  exit();
}
