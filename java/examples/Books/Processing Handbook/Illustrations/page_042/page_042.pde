
// Based on code 4-02 (p. 44) and code 6-01 (p. 63)


size(442, 550);
//size(442, 550, PDF, "page_042.pdf");
background(255);
strokeWeight(0.25);
strokeCap(SQUARE);

float x1 = 0;
float x2 = 0;
float x3 = 0;
float x5 = 0;
float x4 = 0;

for(int i = 0; i <= 50; i++) {
  
  line(width-x2, height * 0.0, width-x2, height * 0.2);
  x2 += 4.5;
  
  line(width-x1, height * 0.2, width-x1, height * 0.4);
  x1 += 9;
  
  line(width-x3, height * 0.4, width-x3, height * 0.6);
  x3 += 6.25;

  line(width-x5, height * 0.6, width-x5, height * 0.8);
  x5 += 3.125;
  
  line(width-x4, height * 0.8, width-x4, height);
  x4 += 5.375;
  
}

