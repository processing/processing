
// Based on code from Shape 1 (p. 23) and code 6-07 (p. 65)


size(713, 938);
//size(713, 938, PDF, "page_022.pdf");
background(255);
int ydiv = height/54;

strokeWeight(0.25);
noStroke();
fill(0);
rectMode(CENTER);

for (int y = 0; y < height; y += ydiv) {
  for (int x = 0; x < width; x += ydiv) {
    ellipse(x, y, 0.5, 0.5);
  }
}

float s = ydiv * 10;
float x = ydiv * 28;

ellipse(x, ydiv * 12, s, s);
rect(x, ydiv*26, ydiv*12, ydiv*12);
fill(255);
ellipse(x, ydiv*26, s, s);

fill(0);
ellipse(x, ydiv*40, s*1.07, s*1.07);
fill(255);
rect(x, ydiv*40, s*.7, s*.7);
