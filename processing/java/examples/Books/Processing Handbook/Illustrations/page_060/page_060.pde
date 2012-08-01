
// Based on the code examples on page 66


size(180, 666);
//size(180, 666, PDF, "page_060.pdf");
background(255);
strokeWeight(0.25);
strokeCap(SQUARE);

for (int y = 0; y <= height; y += 5) { 
  for (int x = 0; x <= width; x += 5) { 
    if (x % 20 == 0) { 
      line(x, y, x-3, y-3); 
    } 
    else { 
      line(x, y, x-3, y+3); 
    } 
  }
}

