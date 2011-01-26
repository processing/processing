
// Based on code 26-04 (p. 231)


int dragX, dragY, moveX, moveY;
boolean record = false;

void setup() {
  size(360, 666);
}

void draw() {
  if (record) {
    beginRecord(PDF, "page_228.pdf");
  }
  
  background(255);
  noFill();
  stroke(0);
  ellipse(dragX, dragY, 200, 200);  // Black circle
  fill(153);
  noStroke();
  ellipse(moveX, moveY, 200, 200);  // Gray circle

  if (record) {
    endRecord();
    record = false; 
  }

}

void mouseMoved() {    // Move gray circle
  moveX = mouseX;
  moveY = mouseY;
}

void mouseDragged() {  // Move black circle
  dragX = mouseX;
  dragY = mouseY;
}

void keyReleased() {
  record = true; 
}

