smooth();
noFill();
randomSeed(0);
strokeWeight(10);
stroke(0, 150);
for (int i = 0; i < 160; i += 10) {
  float begin = radians(i);
  float end = begin + HALF_PI;
  arc(67, 37, i, i, begin, end);
}