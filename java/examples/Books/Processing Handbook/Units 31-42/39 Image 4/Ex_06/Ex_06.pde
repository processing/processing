PImage img = loadImage("topanga.jpg");
background(0);
stroke(255);
strokeWeight(24);
smooth();
line(44, 0, 24, 80);
line(0, 24, 80, 44);
blend(img, 0, 0, 100, 100, 0, 0, 100, 100, DARKEST);