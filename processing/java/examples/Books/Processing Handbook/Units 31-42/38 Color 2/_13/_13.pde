PImage img;
color[] imageColors;

void setup() {
  size(100, 100);
  frameRate(0.5);
  smooth();
  noFill();
  img = loadImage("palette10x10.jpg");
  imageColors = new color[img.width*img.height];
  for (int y = 0; y < img.height; y++) {
    for (int x = 0; x < img.width; x++) {
      imageColors[y*img.height + x] = img.get(x, y);
    }
  }
  imageColors = sortColors(imageColors);
}

void draw() {
  background(255);
  for (int x = 10; x < width; x += 10) {
    int r = int(random(imageColors.length));
    float thick = ((100 - r) / 4.0) + 1.0;
    stroke(imageColors[r]);
    strokeWeight(thick);
    line(x, height, x, height - r + thick);
    line(x, 0, x, height - r - thick);
  }
}
color[] sortColors(color[] colors) {
  color[] sorted = new color[colors.length];
  int num = 0;
  for (int i = 0; i <= 255; i++) {
    for (int j = 0; j < colors.length; j++) {
      if (int(brightness(colors[j])) == i) {
        sorted[num] = colors[j];
        num++;
      }
    }
  }
  return sorted;
}
