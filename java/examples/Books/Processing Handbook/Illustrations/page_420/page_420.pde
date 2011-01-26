
// Based on code 45-04 (p. 424)


void setup() {
  size(513, 666); 
}

void draw() {
  if (mousePressed == true) {
    point(mouseX, mouseY);
    println("hi");
  }
} 

void mousePressed() {
  beginRecord(PDF, "page_420.pdf"); 
  background(255);
  stroke(0);
  strokeCap(ROUND);
}

void mouseReleased() {
  endRecord(); 
}




