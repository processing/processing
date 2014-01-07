/**
 * Many Pages. 
 * 
 * Saves a new page into a PDF file each loop through draw().
 * Pressing the mouse finishes writing the file and exits the program.
 */


import processing.pdf.*;

PGraphicsPDF pdf;

void setup() {
  size(600, 600);
  frameRate(4);
  pdf = (PGraphicsPDF)beginRecord(PDF, "Lines.pdf");
}

void draw() {
  background(255); 
  stroke(0, 20);
  strokeWeight(20.0);
  line(mouseX, 0, width-mouseY, height);
  pdf.nextPage();
}

void mousePressed() {
  endRecord();
  exit();
}
