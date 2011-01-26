
// Based on code 39-03 (p. 349)


size(750, 2775);
background(255);
smooth();
strokeWeight(1.0);

for(int i = 20; i < height-50; i += 50) {
  filter(BLUR, 1);  
  line(0, i, width, i+20);
}

saveFrame("page_346.tif");



 
