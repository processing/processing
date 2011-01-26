
// Based on code 17-14 (p. 142)


size(360, 550);
//size(360, 550, PDF, "page_136.pdf");
background(255);

smooth();
noStroke();
fill(0, 10);

translate(33, 66);              // Set initial offset
for (int i = 0; i < 45; i++) {  // 12 repetitions
  scale(1.02);                  // Accumulate the scaling
  ellipse(25, 90, 200, 200); 
}
