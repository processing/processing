PImage img;
float opacity = 0; // Set opacity to the minimum

void setup() {
  size(100, 100);
  img = loadImage("PT-Teddy-0017.gif");
}

void draw() {
  background(0);
  if (opacity < 255) { // When less than the maximum,
    opacity += 0.5; // increase opacity
  }
  tint(255, opacity);
  image(img, -25, -75);
}
