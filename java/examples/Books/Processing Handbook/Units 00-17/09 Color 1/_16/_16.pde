// Change the saturation, hue and brightness constant
colorMode(HSB);
for (int i = 0; i < 100; i++) {
  stroke(132, i*2.5, 204);
  line(i, 0, i, 100);
}