// Change the hue, saturation and brightness constant
colorMode(HSB);
for (int i = 0; i < 100; i++) {
  stroke(i*2.5, 255, 255);
  line(i, 0, i, 100);
}