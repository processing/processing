/**
 * RandomBook
 * 
 * Creates a 768 page book of random lines.
 */

import processing.pdf.*;

PGraphicsPDF pdf;

void setup() {
  size(594, 842);
  // randomSeed(0);  // Uncomment to make the same book each time
  pdf = (PGraphicsPDF)beginRecord(PDF, "RandomBook.pdf");
  beginRecord(pdf);
}

void draw() {  
  background(255);
  
  for (int i=0; i<100; i++) {
    float r = random(1.0);
    if(r < 0.2) {
      stroke(255); 
    } else {
      stroke(0); 
    }
    float sw = pow(random(1.0), 12);
    strokeWeight(sw * 260); 
    float x1 = random(-200, -100);
    float x2 = random(width+100, width+200);
    float y1 = random(-100, height+100);
    float y2 = random(-100, height+100);
    line(x1, y1, x2, y2);
  }

  if(frameCount == 768) {
    endRecord();
    exit();  // Quit
  } else {
    pdf.nextPage();  // Tell it to go to the next page 
  }
}


