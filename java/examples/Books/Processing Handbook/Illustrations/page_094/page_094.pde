
// Based on code 10-07 (p. 98) and code 35-03 (p. 322)


size(2400, 1200);
background(255);
noStroke();
fill(0);

PImage img = loadImage("rockies-color.jpg");

for(int i = 0; i < width; i += 50) {
  int x = int(random(img.width-50));
  PImage crop = img.get(x, 0, 50, height);
  image(crop, i, 0);
}

//saveFrame("page_094.tif");



