/**
 * Many PDFs. 
 * 
 * Saves one PDF file each each frame while the mouse is pressed.
 * When the mouse is released, the PDF creation stops.
 */


import processing.pdf.*;

boolean savePDF = false;

void setup() {
  size(600, 600);
  frameRate(24);
}

void draw() {
  if(savePDF == true) {
    beginRecord(PDF, "lines" + frameCount + ".pdf");
  }
  background(255); 
  stroke(0, 20);
  strokeWeight(20.0);
  line(mouseX, 0, width-mouseY, height);
  if(savePDF == true) {
    endRecord();
  }
}

void mousePressed() {
  savePDF = true;
}

void mouseReleased() {
  savePDF = false;
}

